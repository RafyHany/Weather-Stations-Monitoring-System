package com.example.Centeral_Station.dto;

import com.example.Centeral_Station.enums.BatteryStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherStatus(
        @JsonProperty("station_id") long stationId,
        @JsonProperty("s_no") long sequenceNumber,
        @JsonProperty("battery_status") BatteryStatus batteryStatus,
        @JsonProperty("status_timestamp") long statusTimestamp,
        @JsonProperty("weather") Weather weather
) {
    public WeatherStatus {
        if (stationId <= 0) {
            throw new IllegalArgumentException("Station ID must be strictly positive.");
        }
        if (sequenceNumber <= 0) {
            throw new IllegalArgumentException("Sequence number must be strictly positive.");
        }
        if (statusTimestamp <= 0) {
            throw new IllegalArgumentException("Timestamp must be a positive Unix epoch time.");
        }
        if (batteryStatus == null) {
            throw new IllegalArgumentException("Battery status is missing or invalid.");
        }
        if (weather == null) {
            throw new IllegalArgumentException("Weather payload cannot be null.");
        }
    }
    public record Weather (
            @JsonProperty("humidity") byte humidity,
            @JsonProperty("temperature") int temperature,
            @JsonProperty("wind_speed") short windSpeed
    ){
        public Weather {
            if (humidity < 0 || humidity > 100) {
                throw new IllegalArgumentException("Humidity must be between 0 and 100%. Got: " + humidity);
            }
            if (windSpeed < 0) {
                throw new IllegalArgumentException("Wind speed cannot be negative. Got: " + windSpeed);
            }
            if (temperature < -460) {
                throw new IllegalArgumentException("Temperature violates physical laws (below absolute zero). Got: " + temperature);
            }
        }
    }
}