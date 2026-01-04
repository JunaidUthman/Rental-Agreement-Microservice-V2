package com.lsiproject.app.rentalagreementmicroservicev2.controllers;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.TenantScoringDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.services.AiModelsService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/rentalAgreement-microservice/ai-models")
public class AiModelsController {

    private final AiModelsService aiModelsService;

    @GetMapping("/consult-score/{idTenant}")
    public TenantScoringDTO getTenantScore(@PathVariable Long idTenant) {
        return aiModelsService.consultTenantScoringModel(idTenant);
    }
}