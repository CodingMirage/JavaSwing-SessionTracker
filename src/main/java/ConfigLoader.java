package org.Miniproject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties properties = new Properties();

    /* loads the db_urls and table names from the config.properties file should be
     kept in the same working directory of the application */
    static {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties from application folder", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
