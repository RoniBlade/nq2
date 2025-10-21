package ru.tedo.Repository;

import ru.tedo.Main;
import ru.tedo.Model.RegionEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DwhRepository {
    private int chankSize = 100;

    public RegionEntity findRegionById(int objectId) throws SQLException {
        DbConnection pg = new DbConnection();
        String sql = "SELECT * FROM loading_fias.dictionary_fias_region WHERE object_id = " + objectId;
        ResultSet rs = pg.executeSqlQuery(sql);

        RegionEntity region = null;
        if (rs.next()) {
            region = new RegionEntity();
            region.setObjectId(rs.getInt("object_id"));
            region.setObjectLevelId(rs.getInt("object_level_id"));
            region.setOperationTypeId(rs.getInt("operation_type_id"));
            region.setObjectGuid(UUID.fromString(rs.getString("object_guid")));
            region.setAddressType(rs.getInt("address_type"));
            region.setFullName(rs.getString("full_name"));
            region.setRegionCode(rs.getInt("region_code"));
            region.setActive(rs.getBoolean("is_active"));
            region.setStatusID(rs.getInt("status_id"));
            region.setPath(rs.getString("path"));

            RegionEntity.AddressDetails details = new RegionEntity.AddressDetails();
            details.setPostalCode(rs.getString("postal_code"));
            details.setIfnsUl(rs.getString("ifns_ul"));
            details.setIfnsFl(rs.getString("ifns_fl"));
            details.setIfnsTul(rs.getString("ifns_tul"));
            details.setIfnsTfl(rs.getString("ifns_tfl"));
            details.setOkato(rs.getString("okato"));
            details.setOktmo(rs.getString("oktmo"));
            details.setKladrCode(rs.getString("kladr_code"));
            details.setCadastralNumber(rs.getString("cadastral_number"));
            details.setApartBuilding(rs.getString("apart_building"));
            details.setRemoveCadastr(rs.getString("remove_cadastr"));
            details.setOktmoBudget(rs.getString("oktmo_budget"));
            details.setIsAdmCapital(rs.getString("is_adm_capital"));
            details.setIsMunCapital(rs.getString("is_mun_capital"));
            region.setAddressDetails(details);

            RegionEntity.SuccessorReference successorRef = new RegionEntity.SuccessorReference();
            successorRef.setObjectId(rs.getInt("successor_object_id"));
            successorRef.setObjectGuid(UUID.fromString(rs.getString("successor_object_guid")));
            region.setSuccessorRef(successorRef);

            RegionEntity.FederalDistrict federalDistrict = new RegionEntity.FederalDistrict();
            federalDistrict.setId(rs.getInt("federal_district_id"));
            federalDistrict.setFullName(rs.getString("federal_district_full_name"));
            federalDistrict.setShortName(rs.getString("federal_district_short_name"));
            federalDistrict.setCenterId(rs.getInt("federal_district_center_id"));
            region.setFederalDistrict(federalDistrict);
        }

        rs.close();
        return region;
    }


    public void insertRegionAddresses(RegionEntity region) throws SQLException {
        DbConnection pg = new DbConnection();

        String sql = "INSERT INTO loading_fias.dictionary_fias_region (" +
                "object_id, object_level_id, operation_type_id, object_guid, address_type, full_name, " +
                "region_code, is_active, status_id, path, postal_code, ifns_ul, ifns_fl, ifns_tul, ifns_tfl, " +
                "okato, oktmo, kladr_code, cadastral_number, apart_building, remove_cadastr, " +
                "oktmo_budget, is_adm_capital, is_mun_capital, successor_object_id, successor_object_guid, " +
                "federal_district_id, federal_district_full_name, federal_district_short_name, " +
                "federal_district_center_id) VALUES (" +
                region.getObjectId() + ", " +
                region.getObjectLevelId() + ", " +
                region.getOperationTypeId() + ", '" +
                region.getObjectGuid() + "', " +
                region.getAddressType() + ", '" +
                region.getFullName().replaceAll("'", "''") + "', " +
                region.getRegionCode() + ", " +
                (region.isActive() ? "TRUE" : "FALSE") + ", '" +
                region.getPath().replaceAll("'", "''") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getPostalCode() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getIfnsUl() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getIfnsFl() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getIfnsTul() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getIfnsTfl() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getOkato() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getOktmo() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getKladrCode() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getCadastralNumber() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getApartBuilding() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getRemoveCadastr() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getOktmoBudget() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getIsAdmCapital() : "") + "', '" +
                (region.getAddressDetails() != null ? region.getAddressDetails().getIsMunCapital() : "") + "', " +
                (region.getSuccessorRef() != null ? region.getSuccessorRef().getObjectId() : "NULL") + ", '" +
                (region.getSuccessorRef() != null ? region.getSuccessorRef().getObjectGuid() : "NULL") + "', " +
                (region.getFederalDistrict() != null ? region.getFederalDistrict().getId() : "NULL") + ", '" +
                (region.getFederalDistrict() != null ? region.getFederalDistrict().getFullName().replaceAll("'", "''") : "") + "', '" +
                (region.getFederalDistrict() != null ? region.getFederalDistrict().getShortName().replaceAll("'", "''") : "") + "', " +
                (region.getFederalDistrict() != null ? region.getFederalDistrict().getCenterId() : "NULL") + ")";

        pg.executeSql(sql);
    }


    public void saveRegionList(List<RegionEntity> regionList) throws SQLException {
        DbConnection pg = new DbConnection();
        StringBuilder sb = new StringBuilder();

        // Шаг 1: Получаем все активные записи из базы данных
        String getActiveRegionsSql = "SELECT object_id FROM loading_fias.dictionary_fias_region WHERE statusID = TRUE";
        ResultSet rs = pg.executeSqlQuery(getActiveRegionsSql);
        Set<Integer> activeRegionsInDb = new HashSet<>();

        while (rs.next()) {
            activeRegionsInDb.add(rs.getInt("object_id"));
        }
        rs.close();

        // Шаг 2: Итерируемся по regionList и обновляем или вставляем записи
        for (RegionEntity region : regionList) {
            int objectId = region.getObjectId();
            boolean recordExists = activeRegionsInDb.contains(objectId);

            if (!recordExists) {
                insertRegionAddresses(region);
            } else {
                // Если запись существует, выполняем обновление и удаляем из списка активных записей
                sb.append("UPDATE loading_fias.dictionary_fias_region SET ")
                        .append("object_level_id = ").append(region.getObjectLevelId()).append(", ")
                        .append("operation_type_id = ").append(region.getOperationTypeId()).append(", ")
                        .append("object_guid = '").append(region.getObjectGuid()).append("', ")
                        .append("address_type = ").append(region.getAddressType()).append(", ")
                        .append("full_name = '").append(region.getFullName().replaceAll("'", "''")).append("', ")
                        .append("region_code = ").append(region.getRegionCode()).append(", ")
                        .append("is_active = TRUE, ")
                        .append("status_id = 0, ")
                        .append("path = '").append(region.getPath().replaceAll("'", "''")).append("', ");

                RegionEntity.AddressDetails details = region.getAddressDetails();
                if (details != null) {
                    sb.append("postal_code = '").append(details.getPostalCode()).append("', ")
                            .append("ifns_ul = '").append(details.getIfnsUl()).append("', ")
                            .append("ifns_fl = '").append(details.getIfnsFl()).append("', ")
                            .append("ifns_tul = '").append(details.getIfnsTul()).append("', ")
                            .append("ifns_tfl = '").append(details.getIfnsTfl()).append("', ")
                            .append("okato = '").append(details.getOkato()).append("', ")
                            .append("oktmo = '").append(details.getOktmo()).append("', ")
                            .append("kladr_code = '").append(details.getKladrCode()).append("', ")
                            .append("cadastral_number = '").append(details.getCadastralNumber()).append("', ")
                            .append("apart_building = '").append(details.getApartBuilding()).append("', ")
                            .append("remove_cadastr = '").append(details.getRemoveCadastr()).append("', ")
                            .append("oktmo_budget = '").append(details.getOktmoBudget()).append("', ")
                            .append("is_adm_capital = '").append(details.getIsAdmCapital()).append("', ")
                            .append("is_mun_capital = '").append(details.getIsMunCapital()).append("', ");
                }

                RegionEntity.SuccessorReference successorRef = region.getSuccessorRef();
                if (successorRef != null) {
                    sb.append("successor_object_id = ").append(successorRef.getObjectId()).append(", ")
                            .append("successor_object_guid = '").append(successorRef.getObjectGuid()).append("', ");
                }

                RegionEntity.FederalDistrict federalDistrict = region.getFederalDistrict();
                if (federalDistrict != null) {
                    sb.append("federal_district_id = ").append(federalDistrict.getId()).append(", ")
                            .append("federal_district_full_name = '").append(federalDistrict.getFullName().replaceAll("'", "''")).append("', ")
                            .append("federal_district_short_name = '").append(federalDistrict.getShortName().replaceAll("'", "''")).append("', ")
                            .append("federal_district_center_id = ").append(federalDistrict.getCenterId()).append(" ");
                }

                sb.append("WHERE object_id = ").append(objectId).append(";\n");

                pg.executeSql(sb.toString());
                sb.setLength(0);  // Очищаем StringBuilder для следующего запроса

                // Удаляем обновленную запись из списка активных записей
                activeRegionsInDb.remove(objectId);
            }
        }

        // Шаг 3: Обновляем все оставшиеся активные записи в базе, которые отсутствуют в regionList
        for (Integer inactiveId : activeRegionsInDb) {
            String updateInactiveSql = "UPDATE loading_fias.dictionary_fias_region SET status_id = 0 WHERE object_id = " + inactiveId;
            pg.executeSql(updateInactiveSql);
        }
    }

}


