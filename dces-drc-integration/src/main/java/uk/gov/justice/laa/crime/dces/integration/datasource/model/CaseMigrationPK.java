package uk.gov.justice.laa.crime.dces.integration.datasource.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * An entity class represents a table in a relational database
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Builder
public class CaseMigrationPK implements Serializable {

    @Id
    private Long maatId;
    @Id
    private Long concorContributionId;
    @Id
    private Long fdcId;


    @Override
    public boolean equals(Object o){
        if( this == o){
            return true;
        }
        if( o == null || getClass() != o.getClass()){
            return false;
        }
        CaseMigrationPK compareToEntity = (CaseMigrationPK) o;
        return Objects.equals(maatId, compareToEntity.getMaatId())
                && Objects.equals(concorContributionId, compareToEntity.getConcorContributionId())
                && Objects.equals(fdcId, compareToEntity.getFdcId());
    }

    @Override
    public int hashCode(){
        return Objects.hash(maatId,concorContributionId,fdcId);
    }
}