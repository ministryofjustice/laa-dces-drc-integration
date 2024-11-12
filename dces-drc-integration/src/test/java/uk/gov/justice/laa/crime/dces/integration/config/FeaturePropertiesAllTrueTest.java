package uk.gov.justice.laa.crime.dces.integration.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.justice.laa.crime.dces.integration.controller.StubAckFromDrcController;
import uk.gov.justice.laa.crime.dces.integration.controller.TempTestController;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = """
        feature.stub-ack-endpoints=true
        feature.temp-test-endpoints=true
        feature.incoming-isolated=true
        feature.outgoing-isolated=true
        feature.outgoing-anonymized=true
        """)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FeaturePropertiesAllTrueTest extends ApplicationTestBase {
    @Autowired
    private FeatureProperties feature;
    @Autowired
    private ApplicationContext context;

    @Test
    void givenFeatureAllTrue_whenCallFeatureMethods_thenMethodsReturnTrue() {
        assertThat(feature.stubAckEndpoints()).isTrue();
        assertThat(feature.tempTestEndpoints()).isTrue();
        assertThat(feature.incomingIsolated()).isTrue();
        assertThat(feature.outgoingIsolated()).isTrue();
        assertThat(feature.outgoingAnonymized()).isTrue();
    }

    @Test
    void givenFeatureAllTrue_whenLookForConditionalBeans_thenAnInstanceIsFound() {
        assertThat(context.getBean(StubAckFromDrcController.class))
                .isInstanceOf(StubAckFromDrcController.class);
        assertThat(context.getBean(TempTestController.class))
                .isInstanceOf(TempTestController.class);
    }
}
