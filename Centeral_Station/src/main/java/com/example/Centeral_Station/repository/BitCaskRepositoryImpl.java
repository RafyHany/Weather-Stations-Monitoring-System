package com.example.Centeral_Station.repository;


import com.example.Centeral_Station.Bitcask.engine.BitcaskEngineI;
import com.example.Centeral_Station.dto.WeatherStatus;
import org.springframework.stereotype.Repository;

@Repository
public class BitCaskRepositoryImpl implements BitCaskRepository {
    private final BitcaskEngineI bitcaskEngine;

    public BitCaskRepositoryImpl(BitcaskEngineI bitcaskEngine) {
        this.bitcaskEngine = bitcaskEngine;
    }

    public void saveLatestReading(WeatherStatus status) {
        System.out.println("Saving to BitCask: Station " + status.stationId());
        bitcaskEngine.put(status);
    }
}
