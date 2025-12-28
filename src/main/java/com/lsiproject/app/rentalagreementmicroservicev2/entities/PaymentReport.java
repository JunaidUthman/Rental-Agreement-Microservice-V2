package com.lsiproject.app.rentalagreementmicroservicev2.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private RentalContract rentalContract;


    private Long tenentID;

    @Column(nullable = false)
    private Double totalPaidSoFar;

    @Column(nullable = false)
    private Double totalExpectedSoFar;

    private Integer paidPeriods;
    private Integer missedPeriods;

    // Storing dates as a String (e.g., "2025-01-01,2025-02-01")
    @Column(columnDefinition = "TEXT")
    private String missedDates;

    private String status; // LATE, UP_TO_DATE, ENDED

    @CreationTimestamp
    private LocalDateTime generatedAt;
}