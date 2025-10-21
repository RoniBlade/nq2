package org.example.dto;

import io.swagger.models.auth.In;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class RoleDto {
    private UUID oid;
    private String nameOrig;
    private String nameNorm;
    private byte[] fullObject; // Можно заменить на Object или Map, если структура сложная
    private UUID tenantRefTargetOid;
    private String tenantRefTargetType;
    private Integer tenantRefRelationId;
    private String lifecycleState;
    private Integer cidSeq; // предполагается числовое значение
    private Integer version;

    // Поля, связанные с ситуациями политики
    private List<String> policySituations; // TODO

    // Подтипы
    private List<String> subtypes; // TODO

    // Информация о полном тексте
    private String fullTextInfo;

    // Расширенные данные (ext) - можно сделать Map
    private String ext;

    // Информация о создателе
    private UUID creatorRefTargetOid;
    private String creatorRefTargetType;
    private Integer creatorRefRelationId;
    private Integer createChannelId;
    private LocalDateTime createTimestamp;

    // Информация о модификаторе
    private UUID modifierRefTargetOid;
    private String modifierRefTargetType;
    private Integer modifierRefRelationId;
    private Integer modifyChannelId;
    private LocalDateTime modifyTimestamp;

    // Временные метки базы данных
    private LocalDateTime dbCreated;
    private LocalDateTime dbModified;

    // Тип объекта
    private String objectType;

    // Финансовый или административный центр
    private String costCenter;

    // Контактные данные
    private String emailAddress;
    private byte[] photo; // если изображение в байтах

    // Локализация и язык
    private String locale;
    private String localityOrig;
    private String localityNorm;
    private String preferredLanguage;

    // Телефон и часовой пояс
    private String telephoneNumber;
    private String timezone;

    // Пароль и его timestamps
    private LocalDateTime passwordCreateTimestamp;
    private LocalDateTime passwordModifyTimestamp;

    // Статусы и временные метки статусов
    private String administrativeStatus;
    private String effectiveStatus;
    private LocalDateTime enableTimestamp;
    private LocalDateTime disableTimestamp;
    private String disableReason;

    // Статус валидности и даты валидности
    private String validityStatus;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime validityChangeTimestamp;

    // Архивные метки и блокировки
    private LocalDateTime archiveTimestamp;
    private String lockoutStatus;

    // Данные в нормализованном виде (может быть JSON или Map)
    private String normalizedData;

    // Автоматическое назначение
    private Boolean autoAssignEnabled;

    // Отображаемое имя и идентификатор
    private String displayNameOrig;
    private String displayNameNorm;
    private String identifier;

    // Запросы и уровень риска
    private Boolean requestable;
    private String riskLevel;
}
