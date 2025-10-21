package com.glt.connector.dto.Account;

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
public class AccountDto {

    private String login;
    private Long serverId;

    private Integer uid;
    private String homeDir;
    private String shell;
    private String description;

    private String data;
    private LocalDateTime accountExpires;
    private LocalDateTime lastSeen;
    private Boolean active;
    private LocalDateTime updatedAt;

    private Set<String> groups;

    private String password;
}
