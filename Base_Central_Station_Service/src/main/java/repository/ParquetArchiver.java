package repository;

import dto.WeatherStatus;

public class ParquetArchiver {
    public void archiveReading(WeatherStatus status) {
        // TODO: Add to batch and write to Parquet file
        System.out.println("Archiving to Parquet: Station " + status.stationId());
    }
}
