package uk.gov.justice.laa.crime.dces.integration.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

public class EnvironmentLoggingListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

  private static final Logger logger = LoggerFactory.getLogger(EnvironmentLoggingListener.class);

  @Override
  public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
    Environment environment = event.getEnvironment();
    // Replace 'my.property' with the actual environment property you want to log
    String propertyValue = environment.getProperty("server.ssl.trust-store-password");
    logger.info("Property 'server.ssl.trust-store-password' value before context initialization: {}", propertyValue);
  }
}