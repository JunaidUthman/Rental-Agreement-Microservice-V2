package com.lsiproject.app.rentalagreementmicroservicev2.services;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.TenantScoreRequest;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.TenantScoringDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.DisputeSummary;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.PaymentReport;
import com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients.TenantScoringAiModel;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.DisputeSummaryRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.PaymentReportRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantScoringAiModelService {

    private final TenantScoringAiModel tenantScoringAi;
    private final DisputeSummaryRepository disputeSummaryRepository;
    private final PaymentReportRepository paymentReportRepository;

    public TenantScoringAiModelService(PaymentReportRepository paymentReportRepository,
                                       TenantScoringAiModel tenantScoringAi,
                                       DisputeSummaryRepository disputeSummaryRepository) {
        this.paymentReportRepository = paymentReportRepository;
        this.tenantScoringAi = tenantScoringAi;
        this.disputeSummaryRepository = disputeSummaryRepository;
    }

    public TenantScoringDTO consultTenantScoringModel(Long id) {
        // 1. Fetch data from DB
        DisputeSummary dispute = disputeSummaryRepository.findById(id).orElse(null);


        PaymentReport report = paymentReportRepository.findByTenentID(id);


        if(dispute!=null && report!=null){
            TenantScoreRequest requestBody = new TenantScoreRequest(
                    report.getMissedPeriods(),
                    dispute.getTotalDisputes()
            );
            return tenantScoringAi.getTenantScore(requestBody);
        }
        else{
            TenantScoreRequest requestBody = new TenantScoreRequest(
                    0,
                    0
            );

            return tenantScoringAi.getTenantScore(requestBody);
        }

    }
}