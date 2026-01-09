package com.lsiproject.app.rentalagreementmicroservicev2.resilience.fallback;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PropertyResponseDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.TypeOfRental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestionnaire de fallback pour PropertyMicroService.
 * Contient la logique de secours en cas d'échec des appels vers le microservice Property.
 */
@Component
public class PropertyFallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(PropertyFallbackHandler.class);

    // ==================================================================================
    // FALLBACKS POUR OPÉRATIONS DE LECTURE
    // Retournent des valeurs par défaut ou des exceptions contrôlées
    // ==================================================================================

    /**
     * Fallback pour getPropertyById.
     * Retourne une exception car nous ne pouvons pas inventer les données d'une propriété.
     */
    public PropertyResponseDTO getPropertyByIdFallback(Long propertyId) {
        log.error("Fallback triggered for getPropertyById({}). PropertyMicroService is unavailable.", propertyId);

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Property service is temporarily unavailable. Please try again later. (Property ID: " + propertyId + ")"
        );
    }

    /**
     * Fallback pour isPropertyAvailable.
     * Retourne false par sécurité : mieux vaut refuser une location que d'accepter une propriété indisponible.
     */
    public boolean isPropertyAvailableFallback(Long propertyId) {
        log.warn("Fallback triggered for isPropertyAvailable({}). Returning false by default.", propertyId);

        // Stratégie conservatrice : considérer la propriété comme indisponible
        return false;
    }

    /**
     * Fallback pour getTypeOfRental.
     * Retourne MONTHLY par défaut (le type le plus courant).
     */
    public TypeOfRental getTypeOfRentalFallback(Long propertyId) {
        log.warn("Fallback triggered for getTypeOfRental({}). Returning MONTHLY by default.", propertyId);

        // Valeur par défaut raisonnable
        return TypeOfRental.MONTHLY;
    }

    // ==================================================================================
    // FALLBACKS POUR OPÉRATIONS DE MODIFICATION
    // Lèvent des exceptions car ces opérations sont critiques et ne peuvent pas échouer silencieusement
    // ==================================================================================

    /**
     * Fallback pour updateAvailability (false ou true).
     * Ces opérations sont critiques : on doit échouer explicitement.
     */
    public Void updateAvailabilityFallback(Long propertyId, boolean availability) {
        String operation = availability ? "mark as available" : "mark as unavailable";

        log.error("Fallback triggered for updateAvailability({}, {}). Operation failed.", propertyId, availability);

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                String.format(
                        "Critical operation failed: Unable to %s property %d. " +
                                "Property service is unavailable. Please contact support or retry later.",
                        operation,
                        propertyId
                )
        );
    }

    // ==================================================================================
    // MÉTHODES UTILITAIRES (Optionnelles)
    // ==================================================================================

    /**
     * Méthode pour logger les statistiques de fallback (peut être appelée périodiquement).
     */
    public void logFallbackStatistics() {
        log.info("PropertyFallbackHandler statistics - check logs for fallback frequency");
    }

    /**
     * Stratégie alternative pour isPropertyAvailable :
     * On pourrait vérifier un cache local si implémenté plus tard.
     *
     * Exemple de structure future :
     *
     * private final PropertyCache propertyCache;
     *
     * public boolean isPropertyAvailableFallbackWithCache(Long propertyId) {
     *     Optional<Boolean> cachedValue = propertyCache.getAvailability(propertyId);
     *     if (cachedValue.isPresent()) {
     *         log.info("Using cached availability for property {}", propertyId);
     *         return cachedValue.get();
     *     }
     *     return false; // Fallback par défaut
     * }
     */
}