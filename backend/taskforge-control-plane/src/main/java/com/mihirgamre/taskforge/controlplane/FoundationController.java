package com.mihirgamre.taskforge.controlplane;

import com.mihirgamre.taskforge.common.config.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foundation")
class FoundationController {

    @GetMapping
    ServiceInfo serviceInfo() {
        return new ServiceInfo("taskforge-control-plane", "0.1.0-SNAPSHOT");
    }
}

