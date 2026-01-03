package com.lsiproject.app.rentalagreementmicroservicev2.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_summaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeSummary {

    @Id
    private Long tenantId; // The Tenant ID is the Primary Key (One summary per tenant)

    // 1. Frequency
    private int totalDisputes;

    // 2. Recency / Gap
    // This stores the number of days elapsed between the *previous* dispute and this *current* one.
    private Integer daysSinceLastDispute;

    // Helper field to calculate the gap. Not necessarily needed for AI input but vital for logic.
    private LocalDateTime lastDisputeDate;
}