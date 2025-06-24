package com.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.template"           // This will scan all com.template packages including modules
})
@EnableScheduling
public class PdfGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdfGeneratorApplication.class, args);
    }
}