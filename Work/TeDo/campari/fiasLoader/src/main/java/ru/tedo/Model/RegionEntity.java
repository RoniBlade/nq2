package ru.tedo.Model;

import lombok.Data;

import java.util.*;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RegionEntity {
    // Основные поля, которые раньше были в RegionAddress
    private int objectId;
    private int objectLevelId;
    private int operationTypeId;
    private UUID objectGuid;
    private int addressType;
    private String fullName;
    private int regionCode;
    private boolean isActive;
    private Integer statusID;
    private String path;

    // Подклассы, описывающие дополнительные данные
    private AddressDetails addressDetails;
    private SuccessorReference successorRef;
    private List<HierarchyElement> hierarchy;
    private FederalDistrict federalDistrict;

    // Подкласс для деталей адреса
    @Data
    public static class AddressDetails {
        private String postalCode;
        private String ifnsUl;
        private String ifnsFl;
        private String ifnsTul;
        private String ifnsTfl;
        private String okato;
        private String oktmo;
        private String kladrCode;
        private String cadastralNumber;
        private String apartBuilding;
        private String removeCadastr;
        private String oktmoBudget;
        private String isAdmCapital;
        private String isMunCapital;
    }

    // Подкласс для ссылки на преемника
    @Data
    public static class SuccessorReference {
        private int objectId;
        private UUID objectGuid;
    }

    // Подкласс для иерархических элементов
    @Data
    public static class HierarchyElement {
        private String objectType;
        private int objectId;
        private int objectLevelId;
        private UUID objectGuid;
        private String fullName;
        private String fullNameShort;
        private String kladrCode;
    }

    // Подкласс для федерального округа
    @Data
    public static class FederalDistrict {
        private int id;
        private String fullName;
        private String shortName;
        private int centerId;
    }
}
