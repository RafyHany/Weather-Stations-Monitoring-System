package com.example.Centeral_Station.controller;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RequestMapping("/bitcask")
@RestController
public class BitcaskController {

    @GetMapping("/test")
    public String testBitcask() {
        System.out.println("testBitcask");
        // Here you would typically call your Bitcask service to perform some operations and return the results. For now, we'll just return a simple message.
        return "Bitcask controller is working!";
    }

}


