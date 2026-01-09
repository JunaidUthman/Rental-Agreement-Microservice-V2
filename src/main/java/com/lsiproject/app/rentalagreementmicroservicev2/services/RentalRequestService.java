package com.lsiproject.app.rentalagreementmicroservicev2.services;


import com.lsiproject.app.rentalagreementmicroservicev2.dtos.*;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.RentalRequest;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.EventType;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.RentalRequestStatus;
import com.lsiproject.app.rentalagreementmicroservicev2.mappers.RentalRequestMapper;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.RentalRequestRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.resilience.circuitbreaker.PropertyCircuitBreaker;
import com.lsiproject.app.rentalagreementmicroservicev2.security.UserPrincipal;
import feign.FeignException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import static com.lsiproject.app.rentalagreementmicroservicev2.enums.RentalRequestStatus.ACCEPTED;
import static com.lsiproject.app.rentalagreementmicroservicev2.enums.RentalRequestStatus.PENDING;

/**
 * Service pour la gestion des demandes de location (RentalRequest).
 */
@Service
public class RentalRequestService {

    private final RentalRequestRepository rentalRequestRepository;
    private final RentalRequestMapper rentalRequestMapper;
    private final PropertyCircuitBreaker propertyCircuitBreaker;
    private final NotificationService notificationService;

    public RentalRequestService(PropertyCircuitBreaker propertyCircuitBreaker,
                                RentalRequestRepository rentalRequestRepository,
                                RentalRequestMapper rentalRequestMapper,
                                NotificationService notificationService
    ) {
        this.rentalRequestRepository = rentalRequestRepository;
        this.rentalRequestMapper = rentalRequestMapper;
        this.propertyCircuitBreaker = propertyCircuitBreaker;
        this.notificationService = notificationService;
    }
    /**
     * Crée une nouvelle demande de location (Étape 1).
     * @param dto Les données de création.
     * @param principal L'utilisateur authentifié (Tenant).
     * @return Le DTO de la demande créée.
     */
    public RentalRequestDto createRequest(@Valid @RequestBody RentalRequestCreationDto dto, UserPrincipal principal) {


        PropertyResponseDTO property;

        try {
            property = propertyCircuitBreaker.getPropertyById(dto.getPropertyId());

            //check if the property is available
            boolean propertyIsAvailable = propertyCircuitBreaker.isPropertyAvailable(property.idProperty());

            if(!propertyIsAvailable){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "this property is not available for rental");

            }

            //check if the user already requested this property
            boolean propertyExists = rentalRequestRepository.existsByPropertyIdAndTenantIdAndStatusIn(
                    property.idProperty(),
                    principal.getIdUser(),
                    new ArrayList<>(Arrays.asList(RentalRequestStatus.PENDING, RentalRequestStatus.ACCEPTED))
            );



            if(propertyExists){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "this property is aready requested for rental");
            }
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }




        // 2. Création de l'entité
        RentalRequest request = new RentalRequest();
        request.setPropertyId(dto.getPropertyId());
        request.setTenantId(principal.getIdUser());
        request.setStatus(PENDING); // Statut initial

        // 3. Sauvegarde et conversion
        request = rentalRequestRepository.save(request);


        notificationService.notify(
                EventType.RENTAL_REQUEST_CREATED, // Assure-toi que cet enum existe
                List.of(property.ownerId()),
                "Nouvelle demande de location",
                "Un locataire est intéressé par votre propriété : " + property.title(),
                Map.of("tenantId", request.getTenantId())
        );
        return rentalRequestMapper.toDto(request);
    }

    /**
     * Récupère toutes les demandes pour une propriété spécifique.
     */
    public List<RentalRequestDto> findAllRequestsForProperty(Long propertyId,UserPrincipal principal) {

        PropertyResponseDTO property;

        try {
            property = propertyCircuitBreaker.getPropertyById(propertyId);

            //check if the property is available
            if(!property.ownerId().equals(principal.getIdUser())){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "the user sending this request should be the owner of the property");
            }

        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }

        return rentalRequestMapper.toDtoList(rentalRequestRepository.findByPropertyId(propertyId));
    }

    /**
     * Récupère toutes les demandes faites par un locataire.
     */
    public List<RentalRequestDto> findAllRequestsForTenant(Long tenantId,UserPrincipal principal) {

        if (!tenantId.equals(principal.getIdUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to view rental requests of another tenant.");
        }
        return rentalRequestMapper.toDtoList(rentalRequestRepository.findByTenantId(tenantId));
    }

    /**
     * Récupère une demande par ID.
     */
    public RentalRequestDto getRequestById(Long requestId) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental request not found."));
        return rentalRequestMapper.toDto(request);
    }


    public List<RentalRequestDto> getAllRequests(UserPrincipal principal){
        if(!principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"the only one who can see all the requests is admin");
        }



        return rentalRequestMapper.toDtoList(rentalRequestRepository.findAll());
    }
    /**
     * Met à jour le statut d'une demande (Étape 2).
     * @param requestId L'ID de la demande.
     * @param dto Le nouveau statut.
     * @param principal L'utilisateur authentifié.
     * @return Le DTO mis à jour.
     */
    @Transactional
    public RentalRequestDto updateRequestStatus(Long requestId, RentalRequestStatusUpdateDto dto, UserPrincipal principal) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental request not found."));


        PropertyResponseDTO property;

        try {
            property = propertyCircuitBreaker.getPropertyById(request.getPropertyId());

            if(!property.ownerId().equals(principal.getIdUser())){
                throw new  ResponseStatusException(HttpStatus.FORBIDDEN, "User sending this request is not the owner of the property");
            }
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }

        // 1. Logique métier pour l'acceptation (Étape 2)
        if (dto.getStatus() == ACCEPTED) { //if the owner accepts the tenet, we should prebent other people from requesting the property
            // Règle métier: Si une requête est ACCEPTED, toutes les autres requêtes PENDING pour cette
            // propriété doivent être REJECTED.
            rejectOtherPendingRequests(request.getPropertyId(), requestId);



            //make the property unnavailable for rental
            propertyCircuitBreaker.updateAvailabilityToFalse(property.idProperty() );

            notificationService.notify(
                    EventType.RENTAL_REQUEST_ACCEPTED,
                    List.of(request.getTenantId()),
                    "Demande acceptée !",
                    "Félicitations ! Votre demande pour '" + property.title() + "' a été acceptée par le propriétaire.",
                    Map.of("propertyId", property.idProperty()) // seulement l'ID de la propriété
            );
        } else if (dto.getStatus() == RentalRequestStatus.REJECTED) {
            // AJOUT : Notification pour le locataire refusé (cas manuel)
            notificationService.notify(
                    EventType.RENTAL_REQUEST_REJECTED,
                    List.of(request.getTenantId()),
                    "Demande refusée",
                    "Malheureusement, votre demande pour '" + property.title() + "' n'a pas été retenue.",
                    Map.of("propertyId", property.idProperty()) // seulement l'ID de la propriété
            );
        }

        // 2. Mise à jour du statut (la seule mise à jour autorisée)
        request.setStatus(dto.getStatus());

        // 3. Sauvegarde et conversion
        request = rentalRequestRepository.save(request);
        return rentalRequestMapper.toDto(request);
    }

    /**
     * Fonction utilitaire pour rejeter les autres demandes en attente pour la même propriété.
     * @param propertyId ID de la propriété.
     * @param acceptedRequestId L'ID de la demande acceptée.
     */
    private void rejectOtherPendingRequests(Long propertyId, Long acceptedRequestId) {
        List<RentalRequest> pendingRequests = rentalRequestRepository.findByPropertyIdAndStatus(
                propertyId, PENDING);


        List<Long> rejectedTenantIds = new ArrayList<>();

        for (RentalRequest req : pendingRequests) {
            if (!req.getIdRequest().equals(acceptedRequestId)) {
                req.setStatus(RentalRequestStatus.REJECTED);
                rentalRequestRepository.save(req);
                rejectedTenantIds.add(req.getTenantId());
            }
        }

        // AJOUT : Notification groupée pour les autres candidats
        if (!rejectedTenantIds.isEmpty()) {
            PropertyResponseDTO prop = propertyCircuitBreaker.getPropertyById(propertyId);
            notificationService.notify(
                    EventType.RENTAL_REQUEST_REJECTED,
                    rejectedTenantIds,
                    "Propriété non disponible",
                    "La propriété '" + prop.title() + "' n'est malheureusement plus disponible.",
                    Map.of("propertyId", propertyId)
            );
        }

    }

    /**
     * Supprime une demande.
     */
    public void deleteRequest(Long requestId, UserPrincipal principal) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental request not found."));

        rentalRequestRepository.delete(request);
    }
}
