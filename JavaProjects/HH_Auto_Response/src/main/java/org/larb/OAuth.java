package org.larb;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;


public class OAuth {

    private static final String URL_GET_TOKEN = "https://api.hh.ru/token";
    private static final String URL_AUTHORIZE = "https://hh.ru/oauth/authorize";
    private static final String URL_SEARCH_VACANCIES = "https://api.hh.ru/vacancies";

    private static final String URL_NEGOTIATIONS = "https://api.hh.ru/negotiations";

    private final OAuthCredentials credentials;


    public OAuth(OAuthCredentials credentials) {
        this.credentials = credentials;
    }


    public void authorize() {
        String authUrl = URL_AUTHORIZE
                + "?response_type=code&client_id=" + credentials.clientId
                + "&redirect_uri=" + credentials.redirectUri;
        System.out.println("Перейдите по ссылке для авторизации: " + authUrl);
        System.out.print("Введите код авторизации из URL после авторизации: ");
        Scanner scanner = new Scanner(System.in);
        credentials.authCode = scanner.next();
    }

    public void getAccessToken() throws IOException, IllegalAccessException {
        if(credentials.authCode == null) throw new IllegalAccessException("Auth code empty");

        URL url = new URL(URL_GET_TOKEN);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("User-Agent", "Student Project (youtobeadam@mail.ru)");
        connection.setDoOutput(true);

        String payload = "grant_type=authorization_code" +
                "&client_id=" + credentials.clientId +
                "&client_secret=" + credentials.clientSecret +
                "&code=" + credentials.authCode +
                "&redirect_uri=" + credentials.redirectUri;

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes());
            os.flush();
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            getErrorMessage(connection);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String response = reader.readLine();

            JSONObject jsonResponse = new JSONObject(response);

            credentials.accessToken = jsonResponse.getString("access_token");
            credentials.refreshToken = jsonResponse.getString("refresh_token");
            credentials.expiresIn = System.currentTimeMillis() / 1000 + jsonResponse.getLong("expires_in");
        }

    }

    public JSONObject searchJobs() throws IOException, IllegalAccessException {
        if (credentials.accessToken == null) throw new IllegalAccessException("Access token is null");

        String searchParamsUrl = URL_SEARCH_VACANCIES +
                "?text=Java+Разработчик" +
                "&area=1" +
                "&employment=full" +
                "&experience=between1And3" +
                "&schedule=fullDay" +
                "&page=1" +
                "&excluded_text=android%2C+javascript" +
                "&per_page=100";

        URL url = new URL(searchParamsUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + credentials.accessToken);
        connection.setRequestProperty("User-Agent", "'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36");
        connection.setDoOutput(true);

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            getErrorMessage(connection);
        }

        JSONObject jsonResponse = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return new JSONObject(response.toString());
        }

    }

    public void responseFullfilment(JSONObject vacancies) throws IOException {
        if (credentials.accessToken == null) throw new IllegalStateException("Access token is missing");


        JSONArray items = vacancies.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            URL url = new URL(URL_NEGOTIATIONS);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + credentials.accessToken);
            connection.setRequestProperty("User-Agent", "'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36");
            connection.setRequestProperty("HH-User-Agent", "Student Project (youtobeadam@mail.ru)");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            connection.setDoOutput(true);

            JSONObject vacancy = items.getJSONObject(i);

            String data = "message=\"Добрый день, Я работаю Java разработчиком около 5 лет. За это время я изучил большое \"\n" +
                    "                            \"количество фреймворков для разработки высоконагруженных приложений, web-сервисов и т.д. \"\n" +
                    "                            \"В их число входят и другие технологии такие как React, NodeJS, Python и т.д. Хотел бы \"\n" +
                    "                            \"рассмотреть возможность сотрудничества. Спасибо. Телефон: 89640277109, \"\n" +
                    "                            \"Telegram: t.me/latadamok\"" +
                    "&resume_id=d51d5a21ff0b1dc0a10039ed1f444861335a70" +
                    "&vacancy_id=" + vacancy.getString("id");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(data.getBytes());
                os.flush();
            }

            if (connection.getResponseCode() == 201) {
                System.out.println("Отклик успешно отправлен на вакансию " + vacancy.getString("id"));
            } else {
                System.out.println("Ошибка при отправке отклика на вакансию " + vacancy.getString("id") + ": " + connection.getResponseCode());
            }

        }
    }

    void getErrorMessage(HttpURLConnection connection) {
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }

            // Включаем ответ от сервера в сообщение исключения
            throw new RuntimeException("Error. Status code " + connection.getResponseCode() +
                    ". Server response: " + errorResponse.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
