package com.mihirgamre.taskforge.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihirgamre.taskforge.common.api.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication(scanBasePackages = "com.mihirgamre.taskforge")
@EntityScan("com.mihirgamre.taskforge.domain")
@EnableJpaRepositories("com.mihirgamre.taskforge.domain")
@Import(GlobalExceptionHandler.class)
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

    @Bean
    ObjectMapper taskforgeObjectMapper() {
        return new ObjectMapper();
    }
}
