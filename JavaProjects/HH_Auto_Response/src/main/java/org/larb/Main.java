package org.larb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

public class Main {


    public static void main(String[] args) throws IOException, IllegalAccessException {

        ConfigLoader configLoader = new ConfigLoader();

        OAuthCredentials credentials = new OAuthCredentials(
                configLoader.getProperty("client_id"),
                configLoader.getProperty("client_secret"),
                configLoader.getProperty("redirect_uri")
        );

        OAuth oAuth = new OAuth(credentials);

        oAuth.authorize();
        oAuth.getAccessToken();
        JSONObject vacancies = oAuth.searchJobs();
        oAuth.responseFullfilment(vacancies);

    }
}