package com.beautybot.whatsappairesponseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BeautyBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeautyBotApplication.class, args);
    }
}


