package com.glt.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(name = "accounts_login_server_id_unique", columnNames = {"login", "server_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String login;

    @Column(name = "server_id", nullable = false, insertable = false, updatable = false)
    private Long serverId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", referencedColumnName = "id", nullable = false)
    private Server server;

    private Integer uid;

    @Column(name = "home_dir")
    private String homeDir;

    private String shell;

    private String description;

    @Column(name = "data", columnDefinition = "text")
    private String data;

    @Column(name = "account_expires")
    private LocalDateTime accountExpires;

    private Boolean active;

    private Boolean dead;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "accounts_in_groups",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups;
}
