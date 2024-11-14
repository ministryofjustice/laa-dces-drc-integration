package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ContributionUpdateRequest extends UpdateRequest{

    @NonNull
    private List<BigInteger> concorContributionIds;

}
