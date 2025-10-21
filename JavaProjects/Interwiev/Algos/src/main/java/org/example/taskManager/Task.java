package org.example.taskManager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Task {
    private String name;
    private String description;

    private boolean done;

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
