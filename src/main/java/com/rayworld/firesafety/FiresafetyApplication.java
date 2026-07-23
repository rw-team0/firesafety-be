package com.rayworld.firesafety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FiresafetyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiresafetyApplication.class, args);
    }

}
