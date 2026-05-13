package com.example.Centeral_Station.repository;

import com.example.Centeral_Station.dto.WeatherStatus;
import jakarta.annotation.PreDestroy;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.io.OutputFile;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ParquetArchiverImpl implements ParquetArchiver {

    private static final int BATCH_SIZE = 10000;
    private static final String BASE_ARCHIVE_DIR = "parquet_archives/";

    private final List<WeatherStatus> buffer;
    private final Schema avroSchema;
    private final DateTimeFormatter dateFormatter;

    public ParquetArchiverImpl() {
        this.buffer = new ArrayList<>(BATCH_SIZE);
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));

        try {
            this.avroSchema = new Schema.Parser().parse(
                    getClass().getClassLoader().getResourceAsStream("Avro/weather_status_schema_v.1.0.0.avsc")
            );
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Failed to load Avro schema for Parquet writer", e);
        }
    }

    public void archiveReading(WeatherStatus status) {
        buffer.add(status);

        if (buffer.size() >= BATCH_SIZE) {
            flushBatch();
        }
    }

    private void flushBatch() {
        if (buffer.isEmpty()) return;

        System.out.println("Flushing batch of " + buffer.size() + " records to Parquet...");

        Map<String, List<WeatherStatus>> partitionedData = buffer.stream()
                .collect(Collectors.groupingBy(this::generatePartitionPath));

        for (Map.Entry<String, List<WeatherStatus>> entry : partitionedData.entrySet()) {
            String partitionDir = entry.getKey();
            List<WeatherStatus> recordsToWrite = entry.getValue();
            writeParquetFile(partitionDir, recordsToWrite);
        }

        buffer.clear();
    }

    private String generatePartitionPath(WeatherStatus status) {
        String dateStr = dateFormatter.format(Instant.ofEpochSecond(status.statusTimestamp()));
        return BASE_ARCHIVE_DIR + "station_id=" + status.stationId() + "/date=" + dateStr + "/";
    }

    private void writeParquetFile(String partitionDir, List<WeatherStatus> records) {
        try {
            Files.createDirectories(Paths.get(partitionDir));
            String fileName = String.format("batch_%d_%s.parquet",
                    System.currentTimeMillis(),
                    UUID.randomUUID());
            Path path = new Path(partitionDir + fileName);
            Configuration conf = new Configuration();
            OutputFile outputFile = HadoopOutputFile.fromPath(path, conf);

            try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(outputFile)
                    .withSchema(avroSchema)
                    .withConf(conf)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .withWriteMode(org.apache.parquet.hadoop.ParquetFileWriter.Mode.OVERWRITE)
                    .build()) {

                for (WeatherStatus status : records) {
                    writer.write(mapToAvroRecord(status));
                }
            }
        } catch (IOException e) {
            System.err.println("Fatal I/O error writing batch to " + partitionDir + ": " + e.getMessage());
        }
    }

    private GenericRecord mapToAvroRecord(WeatherStatus status) {
        GenericRecord record = new GenericData.Record(avroSchema);
        record.put("station_id", status.stationId());
        record.put("s_no", status.sequenceNumber());
        record.put("battery_status", status.batteryStatus().name());
        record.put("status_timestamp", status.statusTimestamp());
        record.put("humidity", status.weather().humidity());
        record.put("temperature", status.weather().temperature());
        record.put("wind_speed", status.weather().windSpeed());

        return record;
    }

    @PreDestroy
    public void shutdown() {
        flushBatch();
    }
}