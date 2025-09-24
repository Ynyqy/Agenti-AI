package com.lusheng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync // <--- 添加此注解
@SpringBootApplication
public class AgenticAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgenticAiApplication.class, args);
    }
}
