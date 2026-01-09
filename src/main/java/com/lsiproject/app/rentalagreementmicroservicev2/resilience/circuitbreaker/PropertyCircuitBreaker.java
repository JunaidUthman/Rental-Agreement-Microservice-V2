package com.lsiproject.app.rentalagreementmicroservicev2.resilience.circuitbreaker;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PropertyResponseDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.TypeOfRental;
import com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients.PropertyMicroService;
import com.lsiproject.app.rentalagreementmicroservicev2.resilience.fallback.PropertyFallbackHandler;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Circuit Breaker spécifique pour les appels vers PropertyMicroService.
 * Cette classe encapsule tous les appels Feign et applique la logique de résilience.
 */
@Component
public class PropertyCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(PropertyCircuitBreaker.class);

    private final PropertyMicroService propertyMicroService;
    private final CircuitBreaker circuitBreaker;
    private final PropertyFallbackHandler fallbackHandler;

    public PropertyCircuitBreaker(
            PropertyMicroService propertyMicroService,
            CircuitBreaker propertyServiceCircuitBreaker,
            PropertyFallbackHandler fallbackHandler) {
        this.propertyMicroService = propertyMicroService;
        this.circuitBreaker = propertyServiceCircuitBreaker;
        this.fallbackHandler = fallbackHandler;

        // Log des événements du circuit breaker
        registerCircuitBreakerEvents();
    }

    /**
     * Exécute un appel protégé par le circuit breaker avec fallback.
     */
    private <T> T executeWithFallback(Supplier<T> supplier, Supplier<T> fallback) {
        Supplier<T> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, supplier);

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.warn("Circuit breaker triggered fallback for PropertyMicroService: {}", e.getMessage());
            return fallback.get();
        }
    }

    /**
     * Récupère une propriété par son ID (lecture).
     */
    public PropertyResponseDTO getPropertyById(Long propertyId) {
        return executeWithFallback(
                () -> propertyMicroService.getPropertyById(propertyId),
                () -> fallbackHandler.getPropertyByIdFallback(propertyId)
        );
    }

    /**
     * Vérifie si une propriété est disponible (lecture).
     */
    public boolean isPropertyAvailable(Long propertyId) {
        return executeWithFallback(
                () -> propertyMicroService.isPropertyAvailable(propertyId),
                () -> fallbackHandler.isPropertyAvailableFallback(propertyId)
        );
    }

    /**
     * Récupère le type de location d'une propriété (lecture).
     */
    public TypeOfRental getTypeOfRental(Long propertyId) {
        return executeWithFallback(
                () -> propertyMicroService.getTypeOfRental(propertyId),
                () -> fallbackHandler.getTypeOfRentalFallback(propertyId)
        );
    }

    /**
     * Met à jour la disponibilité d'une propriété à false (modification).
     * En cas d'échec, on lève une exception car cette opération est critique.
     */
    public void updateAvailabilityToFalse(Long propertyId) {
        executeWithFallback(
                () -> {
                    propertyMicroService.updateAvailabilityToFalse(propertyId);
                    return null;
                },
                () -> fallbackHandler.updateAvailabilityFallback(propertyId, false)
        );
    }

    /**
     * Met à jour la disponibilité d'une propriété à true (modification).
     * En cas d'échec, on lève une exception car cette opération est critique.
     */
    public void updateAvailabilityToTrue(Long propertyId) {
        executeWithFallback(
                () -> {
                    propertyMicroService.updateAvailabilityToTrue(propertyId);
                    return null;
                },
                () -> fallbackHandler.updateAvailabilityFallback(propertyId, true)
        );
    }

    /**
     * Enregistre les événements du circuit breaker pour monitoring.
     */
    private void registerCircuitBreakerEvents() {
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> log.debug("PropertyService call succeeded"))
                .onError(event -> log.warn("PropertyService call failed: {}", event.getThrowable().getMessage()))
                .onStateTransition(event -> log.info("PropertyService Circuit Breaker state changed from {} to {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> log.error("PropertyService call not permitted - Circuit is OPEN"));
    }

    /**
     * Récupère l'état actuel du circuit breaker (utile pour monitoring).
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    /**
     * Force la transition vers l'état CLOSED (utile pour tests ou admin).
     */
    public void resetCircuitBreaker() {
        circuitBreaker.transitionToClosedState();
        log.info("PropertyService Circuit Breaker manually reset to CLOSED state");
    }
}