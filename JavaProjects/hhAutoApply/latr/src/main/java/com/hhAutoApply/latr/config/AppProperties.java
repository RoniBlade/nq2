package com.hhAutoApply.latr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hh")
@Data
public class AppProperties {

    private String client_id;
    private String client_secret;
    private String redirect_uri;
    private String base_url;
    private String oauth_url;
    private String auth_url;
    private String search_url;
    private String apply_url;
    private String access_token;
    private String message;
    private String resumeId;

}
