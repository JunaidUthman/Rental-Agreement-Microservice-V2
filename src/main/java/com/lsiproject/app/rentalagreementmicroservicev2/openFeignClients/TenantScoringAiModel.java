package com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients;

import com.lsiproject.app.rentalagreementmicroservicev2.configuration.FeignConfig;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.TenantScoreRequest;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.TenantScoringDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-model-client", url = "${tenantScoringAiModel.service.url}", configuration = FeignConfig.class)
public interface TenantScoringAiModel {

    @PostMapping("/predict/score")
    TenantScoringDTO getTenantScore(@RequestBody TenantScoreRequest request);
}