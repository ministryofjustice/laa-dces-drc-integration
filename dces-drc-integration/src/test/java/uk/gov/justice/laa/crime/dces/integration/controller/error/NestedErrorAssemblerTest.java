package uk.gov.justice.laa.crime.dces.integration.controller.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NestedErrorAssemblerTest {

  @Test
  void addsSingleNestedField() {
    Map<String, Object> root = new LinkedHashMap<>();

    NestedErrorAssembler.putNested(root, "data.report.detail", "Must not be blank");

    assertThat(root)
        .containsKey("data");

    Map<String, Object> data = castMap(root.get("data"));
    Map<String, Object> report = castMap(data.get("report"));
    List<String> detailMessages = castList(report.get("detail"));

    assertThat(detailMessages).containsExactly("Must not be blank");
  }
  
  @SuppressWarnings("unchecked")
  @Test
  void groupsMultipleMessagesUnderSameLeaf() {
    Map<String, Object> root = new LinkedHashMap<>();

    NestedErrorAssembler.putNested(root, "data.report.detail", "Message 1");
    NestedErrorAssembler.putNested(root, "data.report.detail", "Message 2");

    List<String> detailMessages = castList(
        ((Map<String, Object>) ((Map<String, Object>) root.get("data")).get("report")).get("detail")
    );

    assertThat(detailMessages).containsExactly("Message 1", "Message 2");
  }

  @SuppressWarnings("unchecked")
  @Test
  void handlesSiblingLeavesCorrectly() {
    Map<String, Object> root = new LinkedHashMap<>();

    NestedErrorAssembler.putNested(root, "data.report.detail", "A");
    NestedErrorAssembler.putNested(root, "data.report.level", "B");

    Map<String, Object> report = castMap(
        ((Map<String, Object>) root.get("data")).get("report")
    );

    assertThat(report)
        .containsKeys("detail", "level");

    assertThat(castList(report.get("detail"))).containsExactly("A");
    assertThat(castList(report.get("level"))).containsExactly("B");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Object obj) {
    return (Map<String, Object>) obj;
  }

  @SuppressWarnings("unchecked")
  private List<String> castList(Object obj) {
    return (List<String>) obj;
  }
}