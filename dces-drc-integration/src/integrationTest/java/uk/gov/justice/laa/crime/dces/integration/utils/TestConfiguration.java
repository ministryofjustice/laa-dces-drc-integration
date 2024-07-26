package uk.gov.justice.laa.crime.dces.integration.utils;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class TestConfiguration {

    private static final String BASE_PROPERTIES_FILENAME = "config.properties";
    private static final String LOCAL_PROPERTIES_PREFIX = "local.";
    private static Configuration configuration;

    static {
        initConfig();
    }

    public static String get(String string) {
        return configuration.getString(string);
    }

    public static void initConfig() {
        String propertyFileName =
                System.getenv("HOST_ENV") == null
                        ? LOCAL_PROPERTIES_PREFIX + BASE_PROPERTIES_FILENAME
                        : BASE_PROPERTIES_FILENAME;

        Parameters params = new Parameters();
        File propertiesFile = new File(propertyFileName);

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(propertiesFile).setThrowExceptionOnMissing(true));

        try {
            configuration = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Could not load properties file", e);
        }
    }
}