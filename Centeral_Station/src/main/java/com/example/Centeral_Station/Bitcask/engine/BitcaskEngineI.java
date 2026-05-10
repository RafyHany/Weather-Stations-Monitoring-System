package com.example.Centeral_Station.Bitcask.engine;

import java.util.HashMap;
import java.util.Map;

public interface BitcaskEngineI {
    public void put(long key, String value);

    public String get(long key);

    public HashMap<Long, String> getAll();

}
