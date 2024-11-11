package uk.gov.justice.laa.crime.dces.integration.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.justice.laa.crime.dces.integration.controller.StubAckFromDrcController;
import uk.gov.justice.laa.crime.dces.integration.controller.TempTestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = """
        feature.stub-ack-endpoints=false
        feature.temp-test-endpoints=false
        feature.incoming-isolated=false
        feature.outgoing-isolated=false
        feature.outgoing-anonymized=false
        """)
class FeaturePropertiesAllFalseTest {
    @Autowired
    private FeatureProperties feature;
    @Autowired
    private ApplicationContext context;

    @Test
    void givenFeatureAllFalse_whenCallFeatureMethods_thenMethodsReturnFalse() {
        assertThat(feature.stubAckEndpoints()).isFalse();
        assertThat(feature.tempTestEndpoints()).isFalse();
        assertThat(feature.incomingIsolated()).isFalse();
        assertThat(feature.outgoingIsolated()).isFalse();
        assertThat(feature.outgoingAnonymized()).isFalse();
    }

    @Test
    void givenFeatureAllFalse_whenLookForConditionalBeans_thenThrowsNoSuchBeanDefinition() {
        assertThatThrownBy(() -> context.getBean(StubAckFromDrcController.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(TempTestController.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
