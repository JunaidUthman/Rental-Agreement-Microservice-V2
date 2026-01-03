package com.lsiproject.app.rentalagreementmicroservicev2.services;

import com.lsiproject.app.rentalagreementmicroservicev2.entities.DisputeSummary;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.DisputeSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DisputeSummaryService {

    private final DisputeSummaryRepository repository;

    public DisputeSummaryService(DisputeSummaryRepository repository) {
        this.repository = repository;
    }

    /**
     * Updates or Creates a dispute summary for a tenant.
     * Calculates the gap (days) since the last dispute.
     */
    @Transactional
    public DisputeSummary trackDispute(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();

        DisputeSummary summary = repository.findById(tenantId)
                .orElse(null);

        if (summary == null) {
            // First time dispute
            summary = DisputeSummary.builder()
                    .tenantId(tenantId)
                    .totalDisputes(1)
                    .daysSinceLastDispute(0) // No previous record, so 0 or null
                    .lastDisputeDate(now)
                    .build();
        } else {
            // Calculate days since the PREVIOUS dispute
            long daysDiff = ChronoUnit.DAYS.between(summary.getLastDisputeDate(), now);

            summary.setTotalDisputes(summary.getTotalDisputes() + 1);
            summary.setDaysSinceLastDispute((int) daysDiff);
            summary.setLastDisputeDate(now);
        }

        repository.save(summary);
        return summary;
    }

    public List<DisputeSummary> getAllDisputeSummaries() {
        return repository.findAll();
    }
}