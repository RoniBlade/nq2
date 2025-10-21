package com.glt.connector.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "groups", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"group", "id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false, insertable = false, updatable = false)
    private Long serverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", referencedColumnName = "id", nullable = false)
    private Server server;

    @Column(name = "\"group\"", nullable = false)
    private String group;

    private String description;

    @Column(columnDefinition = "text")
    private String data;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToMany(mappedBy = "groups")
    private Set<Account> accountEntities;
}
