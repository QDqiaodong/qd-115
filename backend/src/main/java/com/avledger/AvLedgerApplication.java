package com.avledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AvLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvLedgerApplication.class, args);
    }

}
