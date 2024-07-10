package uk.gov.justice.laa.crime.dces.integration.model.local;

public enum FdcItemType {
  AGFS("AGFS"), LGFS("LGFS"), NULL("");
  private final String name;

  FdcItemType(String s) {
    name = s;
  }

  public String toString() {
    return this.name;
  }
}
