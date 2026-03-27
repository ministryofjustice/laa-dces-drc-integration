package uk.gov.justice.laa.crime.dces.integration.testdata;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TestDataLoader implements CommandLineRunner {

  @Autowired
  ConcorContributionsRepository concorContributionsRepository;

  @Autowired
  ResourcePatternResolver resourcePatternResolver;

  public static void main(String[] args) {
    var app = new SpringApplication(TestDataLoader.class);
    app.run(args);
  }

  @Override
  public void run(String... args) throws Exception {
    Resource[] resources = resourcePatternResolver.getResources("classpath:error-scenarios/*.xml");

    for (Resource resource : resources) {
      String filename = resource.getFilename();
      System.out.println("Processing file: " + filename);

      // Extract contribution ID from filename (e.g., "contribution-281755985-6391766.xml" -> 281755985)
      String[] parts = filename.split("-");
      if (parts.length >= 2) {
        int contributionId = Integer.parseInt(parts[1]);
        System.out.println("Contribution ID: " + contributionId);

        Optional<ConcorContributionsEntity> contrib = concorContributionsRepository.findById(contributionId);
        if (contrib.isPresent()) {
          // Read XML file contents
          String xmlContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

          // Set currentXml property
          contrib.get().setCurrentXml(xmlContent);
          contrib.get().setStatus(ConcorContributionStatus.ACTIVE);
          contrib.get().setContribFileId(null);
          contrib.get().setDateModified(LocalDate.now());
          contrib.get().setUserModified("dces-phase-2-error-messages");
          concorContributionsRepository.save(contrib.get());

          System.out.println("Updated contribution ID: " + contrib.get().getId());
          System.out.println("XML content set successfully");
        } else {
          System.out.println("Contribution not found for ID: " + contributionId);
        }
      }
    }
  }
}