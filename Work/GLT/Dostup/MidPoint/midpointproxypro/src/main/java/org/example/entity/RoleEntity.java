package org.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "m_role")
public class RoleEntity {

    @Id
    @Column(name = "oid")
    private UUID oid;

    @Column(name = "nameorig")
    private String nameOrig;

    @Column(name = "namenorm")
    private String nameNorm;

    @Column(name = "fullobject")
    private byte[] fullObject; // или Object/Map, если нужно

    @Column(name = "tenantreftargetoid")
    private UUID tenantRefTargetOid;

    @Column(name = "tenantreftargettype")
    private String tenantRefTargetType;

    @Column(name = "tenantreftargetrelationid")
    private Integer tenantRefRelationId;

    @Column(name = "lifecyclestate")
    private String lifecycleState;

    @Column(name = "cidseq")
    private Integer cidSeq;

    @Column(name = "version")
    private Integer version;

    // Здесь пример с ElementCollection
    @Column(name="policy_situation")
    private List<String> policySituations;

    @Column(name="subtype")
    private List<String> subtypes;

    @Column(name = "fulltextinfo")
    private String fullTextInfo;

    @Column(name = "ext", columnDefinition="TEXT") // или JSON, зависит от базы данных
    private String ext;

    // Создатель
    @Column(name = "creatorreftargetoid")
    private UUID creatorRefTargetOid;

    @Column(name = "creatorreftargettype")
    private String creatorRefTargetType;

    @Column(name = "creatorrefrelationid")
    private Integer creatorRefRelationId;

    @Column(name = "createchannelid")
    private Integer createChannelId;

    @Column(name = "createtimestamp")
    private LocalDateTime createTimestamp;

    // Модификатор
    @Column(name = "modifierreftargetoid")
    private UUID modifierRefTargetOid;

    @Column(name = "modifierreftargettype")
    private String modifierRefTargetType;

    @Column(name = "modifierrefrelationid")
    private Integer modifierRefRelationId;

    @Column(name = "modifychannelid")
    private Integer modifyChannelId;

    @Column(name = "modifytimestamp")
    private LocalDateTime modifyTimestamp;

    // Временные метки базы данных
    @Column(name = "dbcreated")
    private LocalDateTime dbCreated;

    @Column(name= "dbmodified")
    private LocalDateTime dbModified;

    // Тип объекта
    @Column(name= "objecttype")
    private String objectType;

    // Центр
    @Column(name= "costcenter")
    private String costCenter;

    // Контакты
    @Column(name= "emailaddress")
    private String emailAddress;

    @Column(name= "photo") // можно оставить byte[] или использовать BLOB тип
    private byte[] photo;

    // Локализация и язык
    @Column(name= "locale")
    private String locale;

    @Column(name= "localityorig")
    private String localityOrig;

    @Column(name= "localitynorm")
    private String localityNorm;

    @Column(name= "preferredlanguage")
    private String preferredLanguage;

    // Телефон и часовой пояс
    @Column(name= "telephonenumber")
    private String telephoneNumber;

    @Column(name= "timezone")
    private String timezone;

    // Пароль timestamps
    @Column(name= "passwordcreatetimestamp")
    private LocalDateTime passwordCreateTimestamp;

    @Column(name= "passwordmodifytimestamp")
    private LocalDateTime passwordModifyTimestamp;

    // Статусы и временные метки статусов
    @Column(name=  "administrativestatus" )
    private String administrativeStatus;

    @Column( name=  "effectivestatus" )
    private String effectiveStatus;

    @Column( name=  "enabletimestamp" )
    private LocalDateTime enableTimestamp;

    @Column( name=  "disabletimestamp" )
    private LocalDateTime disableTimestamp;

    @Column( name=  "disablereason" )
    private String disableReason;

    // Валидность и даты валидности
    @Column( name=  "validitystatus" )
    private String validityStatus;

    @Column( name=  "validfrom" )
    private LocalDateTime validFrom;

    @Column( name=  "validto" )
    private LocalDateTime validTo;

    @Column( name=  "validitychangetimestamp" )
    private LocalDateTime validityChangeTimestamp;

    // Архивные метки и блокировки
    @Column( name=  "archivetimestamp" )
    private LocalDateTime archiveTimestamp;

    @Column( name=  "lockoutstatus" )
    private String lockoutStatus;

    // Нормализованные данные (может быть JSON)
    @Column( name="normalizeddata", columnDefinition="TEXT")
    private String normalizedData;

    // Автоназначение
    @Column( name="autoassignenabled" )
    private Boolean autoAssignEnabled;

    // Отображение и идентификатор
    @Column( name="displaynameorig" )
    private String displayNameOrig;

    @Column( name="displaynamenorm" )
    private String displayNameNorm;

    @Column( name="identifier" )
    private String identifier;

    // Запросы и риск уровень
    @Column( name="requestable" )
    private Boolean requestable;

    @Column( name="risklevel" )
    private String riskLevel;
}
