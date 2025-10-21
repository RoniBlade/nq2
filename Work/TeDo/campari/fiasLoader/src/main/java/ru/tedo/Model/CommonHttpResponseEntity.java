package ru.tedo.Model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CommonHttpResponseEntity {
    Integer responseCode = 0;
    String body = "";
    Map<String,String> headers = new HashMap<>();
}
