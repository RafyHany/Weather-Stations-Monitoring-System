package com.example.Centeral_Station.repository;

import com.example.Centeral_Station.dto.WeatherStatus;

public interface BitCaskRepository {
    void saveLatestReading(WeatherStatus status);
}
