package com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients;

import com.lsiproject.app.rentalagreementmicroservicev2.configuration.FeignConfig;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.AvailabilityDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PropertyResponseDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.TypeOfRental;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "PropertyMicroService",
        url = "http://localhost:8082",
        configuration = FeignConfig.class
)


public interface PropertyMicroService {

    @GetMapping("/api/property-microservice/properties/{id}")
    PropertyResponseDTO getPropertyById(@PathVariable Long id);

    @GetMapping("/api/property-microservice/properties/{id}/isAvailable")
    boolean isPropertyAvailable(@PathVariable Long id);

    @GetMapping("/api/property-microservice/properties/{id}/availability")
    void updateAvailabilityToFalse(@PathVariable Long id);

    @GetMapping("/api/property-microservice/properties/{id}/availability")
    void updateAvailabilityToTrue(@PathVariable Long id);

    @GetMapping("/api/property-microservice/properties/{id}/TypeOfRental")
    TypeOfRental getTypeOfRental(@PathVariable Long id);
}
