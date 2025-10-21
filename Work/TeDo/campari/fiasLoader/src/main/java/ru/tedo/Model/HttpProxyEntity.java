package ru.tedo.Model;

import lombok.Data;

@Data
public class HttpProxyEntity {
    String proxyHost = "";
    Integer proxyPort = 0;
    String proxyUser = "";
    String proxyPassword = "";
}