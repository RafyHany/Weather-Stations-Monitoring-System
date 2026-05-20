package com.example.demo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import jakarta.annotation.PostConstruct;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.LocalInputFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;


@Service
public class ParquetElasticsearchIndexer {
    @Value("${app.storage.parquet.archive-dir}")
    private String archiveDir;

    private static final String INDEX = "weather_statuses";
    private final ElasticsearchClient esClient;
    private final Set<String> indexedFiles = new HashSet<>();
    public ParquetElasticsearchIndexer(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @PostConstruct
    public void createIndexIfMissing() {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(INDEX)).value();
            if (!exists) {
                esClient.indices().create(CreateIndexRequest.of(c -> c
                        .index(INDEX)
                        .mappings(m -> m
                                .properties("station_id",p -> p.long_(l -> l))
                                .properties("s_no",p -> p.long_(l -> l))
                                .properties("battery_status",p -> p.keyword(k -> k))
                                .properties("status_timestamp",p -> p.date(d -> d.format("epoch_second")))
                                .properties("humidity",p -> p.integer(i -> i))
                                .properties("temperature",p -> p.integer(i -> i))
                                .properties("wind_speed",p -> p.integer(i -> i))
                        )
                ));
                System.out.println("[ES Indexer] Created index '" + INDEX + "'");
            }
        } catch (Exception e) {
            System.err.println("[ES Indexer] Could not create index (ES may not be ready yet): " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void indexNewParquetFiles() {
        List<java.nio.file.Path> parquetFiles = findParquetFiles();
        for (java.nio.file.Path filePath : parquetFiles) {
            String key = filePath.toAbsolutePath().toString();
            if (indexedFiles.contains(key)) continue;
            int indexed = indexFile(filePath);
            if (indexed > 0) {
                indexedFiles.add(key);
                System.out.printf("[ES Indexer] Indexed %d records from %s%n",
                        indexed, filePath.getFileName());
            }
        }
    }

    private List<java.nio.file.Path> findParquetFiles() {
        java.nio.file.Path base = Paths.get(archiveDir);
        if (!Files.isDirectory(base)) return List.of();
        try (Stream<java.nio.file.Path> walk = Files.walk(base)) {
            return walk.filter(p -> p.toString().endsWith(".parquet"))
                    .sorted()
                    .toList();

        } catch (IOException e) {
            System.err.println("[ES Indexer] Failed to scan archive dir: "
                    + e.getMessage());
            return List.of();
        }
    }

    private int indexFile(Path filePath) {
        int count = 0;
        try {
           InputFile inputFile = new LocalInputFile(filePath);
            try (var reader = AvroParquetReader.<GenericRecord>builder(inputFile)
                    .withConf(new org.apache.hadoop.conf.Configuration())
                    .build()) {

                BulkRequest.Builder bulk = new BulkRequest.Builder();
                GenericRecord record;

                while ((record = reader.read()) != null) {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("station_id", ((Number) record.get("station_id")).longValue());
                    doc.put("s_no", ((Number) record.get("s_no")).longValue());
                    doc.put("battery_status", String.valueOf(record.get("battery_status")));
                    doc.put("status_timestamp", ((Number) record.get("status_timestamp")).longValue());
                    doc.put("humidity", ((Number) record.get("humidity")).intValue());
                    doc.put("temperature", ((Number) record.get("temperature")).intValue());
                    doc.put("wind_speed", ((Number) record.get("wind_speed")).intValue());

                    String id = doc.get("station_id") + "_" + doc.get("s_no");
                    bulk.operations(op -> op.index(i -> i
                            .index(INDEX)
                            .id(id)
                            .document(doc)
                    ));
                    count++;
                }

                if (count > 0) {
                    esClient.bulk(bulk.build());
                }
            }
        } catch (Exception e) {
            System.err.println("[ES Indexer] " + filePath + " failed: " + e.getMessage());
        }
        return count;
    }

    private Map<String, Object> recordToMap(GenericRecord r) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("station_id", ((Number) r.get("station_id")).longValue());
        doc.put("s_no", ((Number) r.get("s_no")).longValue());
        doc.put("battery_status", r.get("battery_status").toString());
        doc.put("status_timestamp", ((Number) r.get("status_timestamp")).longValue());
        doc.put("humidity", ((Number) r.get("humidity")).intValue());
        doc.put("temperature", ((Number) r.get("temperature")).intValue());
        doc.put("wind_speed", ((Number) r.get("wind_speed")).intValue());
        return doc;
    }
}
