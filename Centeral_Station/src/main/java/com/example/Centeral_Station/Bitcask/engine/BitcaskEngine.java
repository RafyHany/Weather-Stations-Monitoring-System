package com.example.Centeral_Station.Bitcask.engine;

import com.example.Centeral_Station.Bitcask.clock.SystemClock;
import com.example.Centeral_Station.Bitcask.fileHandler.ReaderBitcask;
import com.example.Centeral_Station.Bitcask.fileHandler.WriterBitcask;
import com.example.Centeral_Station.Bitcask.model.BitcaskRecord;
import com.example.Centeral_Station.Bitcask.model.KeyDirRecord;
import com.example.Centeral_Station.dto.WeatherStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class BitcaskEngine implements BitcaskEngineI {
    private final ConcurrentMap<Long, KeyDirRecord> keyDir;

    private final WriterBitcask writerBitcask;
    private final ReaderBitcask readerBitcask;
    private final ObjectMapper objectMapper;


    public BitcaskEngine(WriterBitcask writerBitcask, ReaderBitcask readerBitcask) {
        this.keyDir = new ConcurrentHashMap<>();
        this.writerBitcask = writerBitcask;
        this.readerBitcask = readerBitcask;
        this.objectMapper = new ObjectMapper();
    }




    @Override
    public void put(WeatherStatus weatherStatus) {
        if(weatherStatus == null)
            return;
        try{
            byte[] value = objectMapper.writeValueAsBytes(weatherStatus);
            BitcaskRecord bitcaskRecord = new BitcaskRecord(SystemClock.getCurrentTime(), weatherStatus.stationId(), value);
            KeyDirRecord keyDirRecord = writerBitcask.writeBitcask(bitcaskRecord);
            if(keyDirRecord == null)
                return;
            this.keyDir.put(weatherStatus.stationId(), keyDirRecord);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public WeatherStatus get(long key) {
        KeyDirRecord pointer = keyDir.get(key);
        if (pointer == null) {
            return null; // Station not found
        }

        try {
            BitcaskRecord record = readerBitcask.readBitcask(pointer);
            if (record == null || record.getValue() == null) {
                return null;
            }

            return objectMapper.readValue(record.getValue(), WeatherStatus.class);

        } catch (IOException e) {
            System.err.println("Failed to read status for station: " + key);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<WeatherStatus> getAll() {
        List<WeatherStatus> allStatuses = new ArrayList<>();

        for (Map.Entry<Long, KeyDirRecord> entry : keyDir.entrySet()) {
            try {
                BitcaskRecord record = readerBitcask.readBitcask(entry.getValue());

                if (record != null && record.getValue() != null) {
                    WeatherStatus status = objectMapper.readValue(record.getValue(), WeatherStatus.class);
                    allStatuses.add(status);
                }
            } catch (IOException e) {
                System.err.println("Failed to read record during getAll for station: " + entry.getKey());
            }
        }

        return allStatuses;
    }

    @Scheduled(fixedRate = 10000)
    private void compactBitcask() throws IOException {
        List<Path> dataFiles = this.writerBitcask.getAllDataFiles();
        List<Path> hintFiles = this.writerBitcask.getAllHintFiles();
        this.writerBitcask.createNewFile("_compact");
        HashMap<Long, KeyDirRecord> snapshot = new HashMap<>(this.keyDir);

        for (Map.Entry<Long, KeyDirRecord> entry : snapshot.entrySet()) {
            KeyDirRecord oldPointer = entry.getValue();
            BitcaskRecord record = this.readerBitcask.readBitcask(entry.getValue());


            if (record != null) {
                KeyDirRecord newPointer = this.writerBitcask.writeBitcask(record);
                this.keyDir.replace(entry.getKey(), oldPointer,  newPointer);
            }
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() ->{
            try {
                this.writerBitcask.deleteAllFiles(dataFiles);
                this.writerBitcask.deleteAllFiles(hintFiles);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                scheduler.shutdown(); // Prevent thread leaks
            }
        }, 2, TimeUnit.SECONDS);
    }

    @PostConstruct
    private void recoveryBitcask() throws IOException {
        System.out.println("Starting Bitcask Engine Recovery...");

        try {
            List<Path> hintFiles = this.writerBitcask.getAllHintFiles();
            for (Path hintFile : hintFiles) {
                System.out.println(hintFile.toFile());
                Map<Long, KeyDirRecord> fileRecords = this.readerBitcask.readHintFiles(hintFile);
                this.keyDir.putAll(fileRecords);
            }
            for(Map.Entry<Long, KeyDirRecord> entry : this.keyDir.entrySet()){
                System.out.println("Station ID: " + entry.getKey() + ", File: " + entry.getValue().getFilename() );
            }
            System.out.println("Recovery complete! " + keyDir.size() + " stations loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
