package ru.tedo.Service;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Main;
import ru.tedo.Model.RegionEntity;
import ru.tedo.Model.AddressFieldEntity;
import ru.tedo.Model.CommonHttpResponseEntity;
import ru.tedo.Model.HttpProxyEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessRegion {
    final static Logger logger = LoggerFactory.getLogger(Main.class);
    private HttpProxyEntity proxy = null;

    public ProcessRegion() {
        String isProxy = Main.getProperty("proxy.isProxy");
        if (isProxy.equals("true")) {
            proxy = new HttpProxyEntity();
            proxy.setProxyHost(Main.getProperty("proxy.url"));
            proxy.setProxyPort(Integer.valueOf(Main.getProperty("proxy.port")));
            proxy.setProxyUser(Main.getProperty("proxy.userName"));
            proxy.setProxyPassword(Main.getProperty("proxy.userPassword"));
        }

    }

    public List<RegionEntity> getRegionList() throws Exception {

        CustomSSLContext sslContext = new CustomSSLContext();
        HttpProxyEntity proxy = new HttpProxyEntity();

        Header header = new BasicHeader("master-token", Main.getProperty("fias.key"));

        Header[] headers = new Header[1];

        headers[0] = header;

        CommonHTTPrequestService cs = new CommonHTTPrequestService();
        CommonHttpResponseEntity response = cs.processHttpGetRequest(Main.getProperty("fias.url"), 5, proxy ,sslContext.getSSLContext(), headers);

        if (response.getResponseCode() != 200) {
            logger.error("Server status {}", response.getResponseCode());
        }

        return fetchRegions(response.getBody());

    }

    private List<RegionEntity> fetchRegions(String response) throws JSONException {

        // Парсинг JSON ответа
        JSONObject jsonResponse = new JSONObject(response);

        List<RegionEntity> regionList = new ArrayList<>();

        // Проверка, что в ответе есть массив "addresses"
        if (jsonResponse.has("addresses")) {
            JSONArray jsonArray = jsonResponse.getJSONArray("addresses");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                RegionEntity address = parseRegions(jsonObject);
                regionList.add(address);
            }
        } else {
            System.out.println("API response does not contain 'addresses' array.");
        }

        return regionList;
    }

    public RegionEntity parseRegions(JSONObject jsonObject) throws JSONException {
        RegionEntity regionEntity = new RegionEntity();

        // Установка основных полей RegionEntity
        regionEntity.setObjectId(jsonObject.optInt("object_id", 0));
        regionEntity.setObjectLevelId(jsonObject.optInt("object_level_id", 0));
        regionEntity.setOperationTypeId(jsonObject.optInt("operation_type_id", 0));
        regionEntity.setObjectGuid(UUID.fromString(jsonObject.optString("object_guid", "00000000-0000-0000-0000-000000000000")));
        regionEntity.setAddressType(jsonObject.optInt("address_type", 0));
        regionEntity.setFullName(jsonObject.optString("full_name", ""));
        regionEntity.setRegionCode(jsonObject.optInt("region_code", 0));
        regionEntity.setActive(jsonObject.optBoolean("is_active", true));
        regionEntity.setStatusID(jsonObject.optInt("statusID", 1));
        regionEntity.setPath(jsonObject.optString("path", ""));

        // Парсинг объекта "address_details"
        JSONObject addressDetailsObject = jsonObject.optJSONObject("address_details");
        if (addressDetailsObject != null) {
            RegionEntity.AddressDetails addressDetails = new RegionEntity.AddressDetails();
            addressDetails.setPostalCode(addressDetailsObject.optString("postal_code", ""));
            addressDetails.setIfnsUl(addressDetailsObject.optString("ifns_ul", ""));
            addressDetails.setIfnsFl(addressDetailsObject.optString("ifns_fl", ""));
            addressDetails.setIfnsTul(addressDetailsObject.optString("ifns_tul", ""));
            addressDetails.setIfnsTfl(addressDetailsObject.optString("ifns_tfl", ""));
            addressDetails.setOkato(addressDetailsObject.optString("okato", ""));
            addressDetails.setOktmo(addressDetailsObject.optString("oktmo", ""));
            addressDetails.setKladrCode(addressDetailsObject.optString("kladr_code", ""));
            addressDetails.setCadastralNumber(addressDetailsObject.optString("cadastral_number", ""));
            addressDetails.setApartBuilding(addressDetailsObject.optString("apart_building", ""));
            addressDetails.setRemoveCadastr(addressDetailsObject.optString("remove_cadastr", ""));
            addressDetails.setOktmoBudget(addressDetailsObject.optString("oktmo_budget", ""));
            addressDetails.setIsAdmCapital(addressDetailsObject.optString("is_adm_capital", ""));
            addressDetails.setIsMunCapital(addressDetailsObject.optString("is_mun_capital", ""));
            regionEntity.setAddressDetails(addressDetails);
        }

        // Парсинг объекта "successor_ref"
        JSONObject successorRefObject = jsonObject.optJSONObject("successor_ref");
        if (successorRefObject != null) {
            RegionEntity.SuccessorReference successorRef = new RegionEntity.SuccessorReference();
            successorRef.setObjectId(successorRefObject.optInt("object_id", 0));
            successorRef.setObjectGuid(UUID.fromString(successorRefObject.optString("object_guid", "00000000-0000-0000-0000-000000000000")));
            regionEntity.setSuccessorRef(successorRef);
        }

        // Парсинг массива "hierarchy"
        JSONArray hierarchyArray = jsonObject.optJSONArray("hierarchy");
        if (hierarchyArray != null) {
            List<RegionEntity.HierarchyElement> hierarchyList = new ArrayList<>();
            for (int i = 0; i < hierarchyArray.length(); i++) {
                JSONObject hierarchyObject = hierarchyArray.getJSONObject(i);
                RegionEntity.HierarchyElement hierarchyElement = new RegionEntity.HierarchyElement();
                hierarchyElement.setObjectType(hierarchyObject.optString("object_type", ""));
                hierarchyElement.setObjectId(hierarchyObject.optInt("object_id", 0));
                hierarchyElement.setObjectLevelId(hierarchyObject.optInt("object_level_id", 0));
                hierarchyElement.setObjectGuid(UUID.fromString(hierarchyObject.optString("object_guid", "00000000-0000-0000-0000-000000000000")));
                hierarchyElement.setFullName(hierarchyObject.optString("full_name", ""));
                hierarchyElement.setFullNameShort(hierarchyObject.optString("full_name_short", ""));
                hierarchyElement.setKladrCode(hierarchyObject.optString("kladr_code", ""));
                hierarchyList.add(hierarchyElement);
            }
            regionEntity.setHierarchy(hierarchyList);
        }

        // Парсинг объекта "federal_district"
        JSONObject federalDistrictObject = jsonObject.optJSONObject("federal_district");
        if (federalDistrictObject != null) {
            RegionEntity.FederalDistrict federalDistrict = new RegionEntity.FederalDistrict();
            federalDistrict.setId(federalDistrictObject.optInt("id", 0));
            federalDistrict.setFullName(federalDistrictObject.optString("full_name", ""));
            federalDistrict.setShortName(federalDistrictObject.optString("short_name", ""));
            federalDistrict.setCenterId(federalDistrictObject.optInt("center_id", 0));
            regionEntity.setFederalDistrict(federalDistrict);
        }

        return regionEntity;
    }

}




