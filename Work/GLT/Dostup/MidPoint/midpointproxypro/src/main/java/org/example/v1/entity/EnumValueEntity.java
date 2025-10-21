package org.example.v1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "d_enum_values")
public class EnumValueEntity {

    @Id
    @Column(name = "oid", updatable = false, insertable = false)
    @GeneratedValue
    private UUID oid;

    @Column(name = "enum_type", columnDefinition = "TEXT")
    private String enumtype;

    @Column(name = "enum_value", columnDefinition = "TEXT")
    private String enumvalue;

    public EnumValueEntity(String enumType, String enumValue) {
        this.enumtype = enumType;
        this.enumvalue = enumValue;
    }
}
