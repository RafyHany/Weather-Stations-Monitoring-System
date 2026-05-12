package repository;

import dto.WeatherStatus;

public class BitCaskRepository {
    public void saveLatestReading(WeatherStatus status) {
        // TODO: Implement Riak BitCask writing logic
        System.out.println("Saving to BitCask: Station " + status.stationId());
    }
}
