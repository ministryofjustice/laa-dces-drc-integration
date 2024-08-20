package uk.gov.justice.laa.crime.dces.integration.utils;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class TestConfiguration {

    private static final String PROPERTIES_FILENAME = "application.yaml";
    private static Configuration configuration;

    static {
        initConfig();
    }

    public static String get(String string) {
        return configuration.getString(string);
    }

    public static void initConfig() {
        Parameters params = new Parameters();
        File propertiesFile = new File(PROPERTIES_FILENAME);

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(YAMLConfiguration.class)
                        .configure(params.fileBased().setFile(propertiesFile).setThrowExceptionOnMissing(true));

        try {
            configuration = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Could not load properties file", e);
        }
    }
}