package com.lsiproject.app.rentalagreementmicroservicev2.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PaymentStatusDto {
    private Double totalPaidSoFar;
    private Double totalExpectedSoFar;

    private int paidPeriods;
    private int missedPeriods;

    private List<LocalDate> missedDates;
    private String status;
    private Long tenentId;
}