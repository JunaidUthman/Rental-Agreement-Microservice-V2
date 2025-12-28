package com.lsiproject.app.rentalagreementmicroservicev2.repositories;

import com.lsiproject.app.rentalagreementmicroservicev2.entities.PaymentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentReportRepository extends JpaRepository<PaymentReport, Long> {
    // Helper to find reports for a specific contract
    List<PaymentReport> findByRentalContract_IdContract(Long contractId);
}