package com.lsiproject.app.rentalagreementmicroservicev2.services;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.TenantScoreRequest;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.TenantScoringDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.DisputeSummary;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.PaymentReport;
import com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients.AiModelClient;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.DisputeSummaryRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.PaymentReportRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AiModelsService {

    private final AiModelClient aiModelClient;
    private final DisputeSummaryRepository disputeSummaryRepository;
    private final PaymentReportRepository paymentReportRepository;

    public AiModelsService(PaymentReportRepository paymentReportRepository,
                           AiModelClient aiModelClient,
                           DisputeSummaryRepository disputeSummaryRepository) {
        this.paymentReportRepository = paymentReportRepository;
        this.aiModelClient = aiModelClient;
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

            return aiModelClient.getTenantScore(requestBody);
        }
        else{
            TenantScoreRequest requestBody = new TenantScoreRequest(
                    0,
                    0
            );

            return aiModelClient.getTenantScore(requestBody);
        }

    }
}