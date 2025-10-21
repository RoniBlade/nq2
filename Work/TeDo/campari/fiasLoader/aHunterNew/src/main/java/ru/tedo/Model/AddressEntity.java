package ru.tedo.Model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AddressEntity {
    private Integer addressID = 0;
    private String originString = "";
    private String preatyString = "";
    private String fiasCode = "";
    private String fiasHouseID = "";
    private String house = "";
    private String fiasObjectLevel = "";
    private String fiasObjectID = "";
    private String city = "";
    private Map<String,AddressFieldEntity> addressFields = new HashMap<>();
    private Double longitude = 0.0;
    private Double latitude = 0.0;
    private Double quality = 0.0;
    private Integer statusID = 0;
}
