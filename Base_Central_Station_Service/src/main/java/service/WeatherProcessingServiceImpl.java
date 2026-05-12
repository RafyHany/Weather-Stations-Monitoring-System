package service;

import dto.WeatherStatus;
import repository.BitCaskRepository;
import repository.ParquetArchiver;

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
