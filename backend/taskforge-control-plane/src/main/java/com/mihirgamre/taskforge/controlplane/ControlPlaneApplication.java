package com.mihirgamre.taskforge.controlplane;

import com.mihirgamre.taskforge.common.api.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.mihirgamre.taskforge")
@EntityScan("com.mihirgamre.taskforge.domain")
@EnableJpaRepositories("com.mihirgamre.taskforge.domain")
@Import(GlobalExceptionHandler.class)
public class ControlPlaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
}
