package com.example.Centeral_Station.Bitcask.engine;

import com.example.Centeral_Station.dto.WeatherStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BitcaskEngineI {
    public void put(WeatherStatus  weatherStatus);

    public WeatherStatus get(long key);

    public List<WeatherStatus> getAll();

}
