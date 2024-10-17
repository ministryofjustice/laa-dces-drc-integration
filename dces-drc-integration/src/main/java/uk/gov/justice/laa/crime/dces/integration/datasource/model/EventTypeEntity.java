package uk.gov.justice.laa.crime.dces.integration.datasource.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An entity class represents a table in a relational database
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "event_type")
public class EventTypeEntity {

    @Id
    private Integer id;
    private String description;

}