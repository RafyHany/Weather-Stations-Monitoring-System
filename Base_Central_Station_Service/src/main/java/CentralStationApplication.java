import messaging.CentralStationConsumer;
import repository.BitCaskRepository;
import repository.ParquetArchiver;
import service.WeatherProcessingService;

public class CentralStationApplication {

    public static void main(String[] args) {
        System.out.println("Initializing Base Central Station...");

        BitCaskRepository bitCaskRepo = new BitCaskRepository();
        ParquetArchiver parquetArchiver = new ParquetArchiver();

        WeatherProcessingService weatherService = new WeatherProcessingService(bitCaskRepo, parquetArchiver);

        CentralStationConsumer consumer = new CentralStationConsumer(weatherService);

        consumer.startConsuming();
    }
}