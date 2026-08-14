package com.mihirgamre.taskforge.scheduler;

import com.mihirgamre.taskforge.common.config.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/foundation")
class SchedulerController {

    @GetMapping
    ServiceInfo serviceInfo() {
        return new ServiceInfo("taskforge-scheduler", "0.1.0-SNAPSHOT");
    }
}

