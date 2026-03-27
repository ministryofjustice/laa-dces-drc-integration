package uk.gov.justice.laa.crime.dces.integration.testdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcorContributionsRepository extends JpaRepository<ConcorContributionsEntity, Integer> {

}