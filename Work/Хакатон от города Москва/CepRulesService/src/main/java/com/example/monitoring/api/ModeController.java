package com.example.monitoring.api;

import com.example.monitoring.service.ModeManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mode")
@RequiredArgsConstructor
public class ModeController {

    private final ModeManager modeManager;

    @GetMapping
    public String getMode() {
        return modeManager.getMode();
    }

    @PostMapping("/{mode}")
    public String switchMode(@PathVariable("mode") String mode) {
        return modeManager.switchMode(mode);
    }

    @PostMapping("/process")
    public String process() {
        modeManager.processMessages();
        return "Processing started in mode: " + modeManager.getMode();
    }
}
