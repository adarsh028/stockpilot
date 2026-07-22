package com.stockpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class StockpilotApplication {

    public static void main(String[] args) {
        // Persist all timestamps in UTC and ensure the JDBC driver sends a timezone
        // id PostgreSQL accepts (the JVM default can be a legacy id like "Asia/Calcutta").
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(StockpilotApplication.class, args);
    }
}
