package com.lsiproject.app.rentalagreementmicroservicev2.repositories;

import com.lsiproject.app.rentalagreementmicroservicev2.entities.DisputeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeSummaryRepository extends JpaRepository<DisputeSummary, Long> {
}