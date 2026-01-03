package com.lsiproject.app.rentalagreementmicroservicev2.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TenantScoreRequest {

    @JsonProperty("missedPeriods")
    private int missedPeriods;

    @JsonProperty("totalDisputes")
    private int totalDisputes;

    public TenantScoreRequest(int missedPeriods, int totalDisputes) {
        this.missedPeriods = missedPeriods;
        this.totalDisputes = totalDisputes;
    }

    // Getters and Setters (if needed by Jackson, usually Constructor is enough or standard Getters)
    public int getMissedPeriods() { return missedPeriods; }
    public int getTotalDisputes() { return totalDisputes; }
}