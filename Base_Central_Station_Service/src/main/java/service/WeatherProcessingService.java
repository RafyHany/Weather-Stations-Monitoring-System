package service;

public interface WeatherProcessingService {
        void processStatus(dto.WeatherStatus status);
        void handleFailedMessage(String rawMessage, Exception e);
}
