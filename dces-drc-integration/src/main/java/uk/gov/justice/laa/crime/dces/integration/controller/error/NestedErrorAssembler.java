package uk.gov.justice.laa.crime.dces.integration.controller.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NestedErrorAssembler {

  private NestedErrorAssembler() {
  }

  @SuppressWarnings("unchecked")
  public static void putNested(
      Map<String, Object> root,
      String fieldPath,
      String message
  ) {
    String[] segments = fieldPath.split("\\.");
    Map<String, Object> currentLevel = root;

    for (int i = 0; i < segments.length - 1; i++) {
      currentLevel = (Map<String, Object>) currentLevel.computeIfAbsent(
          segments[i],
          k -> new LinkedHashMap<>()
      );
    }

    String leaf = segments[segments.length - 1];

    List<String> messages = (List<String>) currentLevel.computeIfAbsent(
        leaf,
        k -> new ArrayList<>()
    );

    messages.add(message);
  }
}
