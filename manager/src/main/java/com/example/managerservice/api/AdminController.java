package com.example.managerservice.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    private final List<String> allowedContainers = List.of(
            "manager", "rabbitmq",
            "mongo-primary", "mongo-secondary1", "mongo-secondary2",
            "ris-fault-tolerance-master-2-worker-1",
            "ris-fault-tolerance-master-2-worker-2"
    );

    @PostMapping("/{service}/{action}")
    public ResponseEntity<String> controlService(@PathVariable String service, @PathVariable String action) {
        if (!allowedContainers.contains(service) ||
                !(action.equals("start") || action.equals("stop"))) {
            return ResponseEntity.badRequest().body("Недопустимый сервис или действие");
        }

        ProcessBuilder pb = new ProcessBuilder("docker", action, service);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("docker {} {} успешно", action, service);
                return ResponseEntity.ok("Успешно: docker " + action + " " + service + "\n" + output);
            } else {
                log.warn("Ошибка при выполнении docker {} {}", action, service);
                return ResponseEntity.status(500).body("Ошибка: " + output);
            }
        } catch (Exception e) {
            log.error("Исключение при управлении docker: {}", e.getMessage());
            return ResponseEntity.status(500).body("Исключение: " + e.getMessage());
        }
    }

    @GetMapping("/status/{service}")
    public ResponseEntity<String> getStatus(@PathVariable String service) {
        if (!allowedContainers.contains(service)) {
            return ResponseEntity.badRequest().body("Неизвестный контейнер");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "inspect", "-f", "{{.State.Running}}", service);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            if (result != null && result.trim().equals("true")) {
                return ResponseEntity.ok("running");
            } else {
                return ResponseEntity.ok("stopped");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error: " + e.getMessage());
        }
    }
}
