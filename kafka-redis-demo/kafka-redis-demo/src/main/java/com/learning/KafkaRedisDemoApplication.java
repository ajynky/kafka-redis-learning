package com.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KafkaRedisDemoApplication {

    static void main(String[] args) {
        SpringApplication.run(KafkaRedisDemoApplication.class, args);
    }
}
