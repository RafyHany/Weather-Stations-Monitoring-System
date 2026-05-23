package com.example.Centeral_Station.service;

import com.example.Centeral_Station.dto.WeatherStatus;

public interface WeatherProcessingService {
        void processStatus(WeatherStatus status);
        void handleFailedMessage(String rawMessage, Exception e);
}
