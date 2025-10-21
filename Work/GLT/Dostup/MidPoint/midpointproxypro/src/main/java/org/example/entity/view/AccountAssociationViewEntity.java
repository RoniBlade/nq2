package org.example.entity.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "glt_get_accounts_association_v")
public class AccountAssociationViewEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "entitlements")
    private String entitlements;

}
