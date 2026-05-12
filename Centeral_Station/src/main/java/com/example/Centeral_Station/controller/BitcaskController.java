package com.example.Centeral_Station.controller;


import com.example.Centeral_Station.Bitcask.engine.BitcaskEngine;
import com.example.Centeral_Station.Bitcask.model.BitcaskRecord;
import com.example.Centeral_Station.dto.WeatherStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@CrossOrigin("*")
@RequestMapping("/bitcask")
@RestController
public class BitcaskController {


    private final BitcaskEngine bitcaskEngine;

    public BitcaskController(BitcaskEngine bitcaskEngine) {
        this.bitcaskEngine = bitcaskEngine;
    }

    @GetMapping("/test")
    public String testBitcask() throws IOException {
        return "Bitcask test successful";
    }

    @GetMapping("/{key}")
    public ResponseEntity<WeatherStatus> getWeatherStatus(@PathVariable String key) throws IOException {
        long stationId;

        try {
            stationId = Long.parseLong(key);
            System.out.println(stationId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build(); // Invalid key format
        }

        WeatherStatus weatherStatus = bitcaskEngine.get(stationId);
        if (weatherStatus == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(weatherStatus);
    }

    @GetMapping("/")
    public ResponseEntity<List<WeatherStatus>> getBitcaskRecords() throws IOException {
        try{
            System.out.println("getBitcaskRecords");
            List<WeatherStatus> res = bitcaskEngine.getAll();
            return ResponseEntity.ok(res);
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/put")
    public ResponseEntity<String> put(@RequestBody WeatherStatus weatherStatus) throws IOException {
        try{
            System.out.println("put weather status");
            bitcaskEngine.put(weatherStatus);
            return ResponseEntity.ok("Weather status added successfully");
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to add weather status");
        }
    }







}


