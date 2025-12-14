package com.winter.labforgeai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.winter.labforgeai.mapper")
public class LabForgeAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabForgeAiApplication.class, args);
    }

}
