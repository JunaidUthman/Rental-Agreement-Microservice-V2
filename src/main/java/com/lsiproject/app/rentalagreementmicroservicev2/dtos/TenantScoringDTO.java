package com.lsiproject.app.rentalagreementmicroservicev2.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TenantScoringDTO {

    @JsonProperty("trust_score")
    private int trustScore;

    @JsonProperty("risk_category")
    private String riskCategory;

    @JsonProperty("recommendation")
    private String recommendation;

}