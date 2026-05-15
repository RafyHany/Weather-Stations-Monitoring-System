package com.weather;

/**
 * Main — the single entry point for the fat JAR.
 *
 * Dispatches to either WeatherStation or RainDetector based on
 * the first command-line argument.  This lets one Docker image
 * serve both roles (just change the CMD in the Dockerfile).
 *
 * Usage:
 *   java -jar app.jar station   → runs as a weather station
 *   java -jar app.jar detector  → runs as the rain detector
 */
public class Main {
    public static void main(String[] args) throws Exception {
        String mode = (args.length > 0) ? args[0] : "station";

        switch (mode) {
            case "station"  -> WeatherStation.main(args);
            case "detector" -> RainDetector.main(args);
            default -> {
                System.err.println("Unknown mode: " + mode);
                System.err.println("Usage: java -jar app.jar [station|detector]");
                System.exit(1);
            }
        }
    }
}
