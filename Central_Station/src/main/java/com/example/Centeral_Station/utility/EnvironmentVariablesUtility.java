package com.example.Centeral_Station.utility;

public class EnvironmentVariablesUtility {
    public static String getEnvOrDefault(String envName, String defaultValue) {
        String envValue = System.getenv(envName);
        return (envValue != null && !envValue.isBlank()) ? envValue : defaultValue;
    }
}
