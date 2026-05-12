import messaging.CentralStationConsumer;
import messaging.CentralStationConsumerImpl;
import repository.BitCaskRepository;
import repository.ParquetArchiver;
import service.WeatherProcessingService;
import service.WeatherProcessingServiceImpl;

public class CentralStationApplication {

    public static void main(String[] args) {
        System.out.println("Initializing Base Central Station...");

        BitCaskRepository bitCaskRepo = new BitCaskRepository();
        ParquetArchiver parquetArchiver = new ParquetArchiver();

        WeatherProcessingService weatherService = new WeatherProcessingServiceImpl(bitCaskRepo, parquetArchiver);

        CentralStationConsumer consumer = new CentralStationConsumerImpl(weatherService);

        consumer.startConsuming();
    }
}