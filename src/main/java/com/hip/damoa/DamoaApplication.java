package com.hip.damoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DamoaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DamoaApplication.class, args);
    }

}