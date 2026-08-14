package com.mihirgamre.taskforge.worker;

import com.mihirgamre.taskforge.common.config.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/foundation")
class WorkerController {

    @GetMapping
    ServiceInfo serviceInfo() {
        return new ServiceInfo("taskforge-worker", "0.1.0-SNAPSHOT");
    }
}

