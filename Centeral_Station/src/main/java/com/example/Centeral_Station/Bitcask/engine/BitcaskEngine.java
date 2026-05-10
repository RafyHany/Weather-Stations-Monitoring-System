package com.example.Centeral_Station.Bitcask.engine;

import com.example.Centeral_Station.Bitcask.model.KeyDirRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BitcaskEngine implements BitcaskEngineI {
    private final ConcurrentHashMap<Long, KeyDirRecord> keyDirRecords = new ConcurrentHashMap<>();



    @Override
    public String get(long key) {
        return "";
    }

    @Override
    public void put(long key, String value) {

    }

    @Override
    public HashMap<Long, String> getAll() {
        return new HashMap<>();
    }
}
