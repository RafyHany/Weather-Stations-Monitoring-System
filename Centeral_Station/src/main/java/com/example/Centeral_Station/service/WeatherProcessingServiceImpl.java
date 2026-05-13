package com.example.Centeral_Station.service;

import com.example.Centeral_Station.dto.WeatherStatus;
import com.example.Centeral_Station.repository.BitCaskRepository;
import com.example.Centeral_Station.repository.ParquetArchiver;
import org.springframework.stereotype.Service;

@Service
public class WeatherProcessingServiceImpl implements WeatherProcessingService {
    private final BitCaskRepository bitCaskRepo;
    private final ParquetArchiver parquetArchiver;

    public WeatherProcessingServiceImpl(BitCaskRepository bitCaskRepo, ParquetArchiver parquetArchiver) {
        this.bitCaskRepo = bitCaskRepo;
        this.parquetArchiver = parquetArchiver;
    }

    public void processStatus(WeatherStatus status) {
        bitCaskRepo.saveLatestReading(status);
        parquetArchiver.archiveReading(status);
    }

    public void handleFailedMessage(String rawMessage, Exception e) {
        System.err.println("Message failed validation: " + e.getMessage());
    }
}
