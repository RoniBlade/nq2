package com.glt.connector.dto;

import com.glt.connector.model.Account;
import com.glt.connector.model.Server;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDto {

    private Long id;

    private String group;

    private Long serverId;

    private String description;

    private String data;

    private LocalDateTime lastSeen = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
