package uk.gov.justice.laa.crime.dces.integration.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

  @Bean
  public WebMvcConfigurer corsConfigurer(Environment env) {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = new ArrayList<>();
        for (int i = 0; ; i++) {
          String host = env.getProperty("MON_HOST_" + i);
          if (host == null) {
            break;
          }
          allowedOrigins.add(host);
        }

        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
      }
    };
  }
}