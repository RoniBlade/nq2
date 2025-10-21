package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "m_user")
public class UserEntity {

    @Id
    @Column(name = "oid")
    private UUID oid;

    @Lob
    @Column(name = "photo", columnDefinition = "bytea")
    private byte[] photo;
}
