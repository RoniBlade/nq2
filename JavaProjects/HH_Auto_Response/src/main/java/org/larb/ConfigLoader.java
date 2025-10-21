package org.larb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {

    private final Properties properties;
    String propertiesPath = "src/main/resources/application.properties";
    public ConfigLoader() throws IOException {
        properties = new Properties();
        try(FileInputStream fis = new FileInputStream(propertiesPath)) {
            properties.load(fis);
        }

    }

    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }
}
