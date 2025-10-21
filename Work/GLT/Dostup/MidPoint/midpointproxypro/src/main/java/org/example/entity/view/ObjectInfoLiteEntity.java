package org.example.entity.view;

import jakarta.persistence.*;
import lombok.*;
import org.example.model.ObjectTypeEnum;


import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "glt_object_info_lite")
public class ObjectInfoLiteEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "oid")
    private UUID oid;

    @Column(name = "objecttype")
    private String objectType;

    @Column(name = "object_name")
    private String objectName;

    @Column(name = "object_fullname")
    private String objectFullName;

    @Column(name = "object_description")
    private String objectDescription;

    @Column(name = "user_title")
    private String userTitle;

    @Column(name = "user_emailaddress")
    private String userEmailAddress;

    @Column(name = "user_telephonenumber")
    private String userTelephoneNumber;

    @Column(name = "user_photo")
    private byte[] photo;
}
