package com.example.Centeral_Station.clock;

public class SystemClock {
    public static long getCurrentTime(){
        return java.time.Instant.now().toEpochMilli();
    }
}
