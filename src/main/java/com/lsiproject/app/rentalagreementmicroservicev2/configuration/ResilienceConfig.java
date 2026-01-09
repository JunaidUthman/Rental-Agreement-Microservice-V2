package com.lsiproject.app.rentalagreementmicroservicev2.configuration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration globale de Resilience4j pour les Circuit Breakers.
 * Cette classe centralise la configuration de tous les circuit breakers du microservice.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Configuration du Circuit Breaker pour PropertyMicroService.
     *
     * Paramètres configurés :
     * - slidingWindowSize: 10 appels pour calculer le taux d'échec
     * - failureRateThreshold: 50% d'échecs déclenchent l'ouverture
     * - waitDurationInOpenState: 5 secondes avant de passer en half-open
     * - permittedNumberOfCallsInHalfOpenState: 3 appels de test en half-open
     * - minimumNumberOfCalls: 5 appels minimum avant calcul du taux d'échec
     */
    @Bean
    public CircuitBreaker propertyServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // Enregistrer toutes les exceptions comme des échecs
                .recordExceptions(Exception.class)
                .build();

        return circuitBreakerRegistry.circuitBreaker("propertyService", config);
    }

    /**
     * Template pour ajouter d'autres circuit breakers.
     * Exemple pour un futur UserMicroService :
     *
     * @Bean
     * public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
     *     CircuitBreakerConfig config = CircuitBreakerConfig.custom()
     *             .slidingWindowSize(10)
     *             .failureRateThreshold(50.0f)
     *             .waitDurationInOpenState(Duration.ofSeconds(5))
     *             .build();
     *
     *     return circuitBreakerRegistry.circuitBreaker("userService", config);
     * }
     */
}