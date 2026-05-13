package com.example.Centeral_Station.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BatteryStatus {
    @JsonProperty("low")
    LOW,

    @JsonProperty("medium")
    MEDIUM,

    @JsonProperty("high")
    HIGH
}