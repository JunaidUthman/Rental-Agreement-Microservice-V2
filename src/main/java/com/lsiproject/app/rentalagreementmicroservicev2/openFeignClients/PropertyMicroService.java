package com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients;

import com.lsiproject.app.rentalagreementmicroservicev2.configuration.FeignConfig;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.AvailabilityDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PropertyResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "PropertyMicroService",
        url = "http://localhost:8084",
        configuration = FeignConfig.class
)


public interface PropertyMicroService {

    @GetMapping("/api/property-microservice/properties/{id}")
    PropertyResponseDTO getPropertyById(@PathVariable Long id);

    @GetMapping("/api/property-microservice/properties/{id}/isAvailable")
    boolean isPropertyAvailable(@PathVariable Long id);

    @GetMapping("/api/property-microservice/properties/{id}/availability")
    void updateAvailabilityToFalse(@PathVariable Long id);
}
