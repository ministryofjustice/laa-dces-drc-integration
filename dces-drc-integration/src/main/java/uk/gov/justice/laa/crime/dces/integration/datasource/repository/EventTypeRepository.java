package uk.gov.justice.laa.crime.dces.integration.datasource.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.EventTypeEntity;

@Repository
public interface EventTypeRepository extends JpaRepository<EventTypeEntity, Integer> {

    @Cacheable("eventTypes")
    EventTypeEntity getEventTypeEntityByDescriptionEquals(String s);

}
