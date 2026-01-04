package com.lsiproject.app.rentalagreementmicroservicev2.controllers;


import com.lsiproject.app.rentalagreementmicroservicev2.dtos.RentalRequestCreationDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.RentalRequestDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.RentalRequestStatusUpdateDto;
import com.lsiproject.app.rentalagreementmicroservicev2.security.UserPrincipal;
import com.lsiproject.app.rentalagreementmicroservicev2.services.RentalRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


/**
 * Contrôleur REST pour gérer le cycle de vie des demandes de location (RentalRequest).
 */
@RestController
@RequestMapping("/api/rentalAgreement-microservice/rental-requests")
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    public RentalRequestController(RentalRequestService rentalRequestService) {
        this.rentalRequestService = rentalRequestService;
    }

    /**
     * POST /api/v1/rental-requests
     * Étape 1: Crée une nouvelle demande de location (Statut PENDING).
     * Autorisation : Tout utilisateur authentifié.
     */
    @PostMapping
    public ResponseEntity<RentalRequestDto> createRentalRequest(
            @Valid @RequestBody RentalRequestCreationDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        RentalRequestDto createdRequest = rentalRequestService.createRequest(dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<RentalRequestDto>> getRequestsForProperty(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<RentalRequestDto> requests =
                rentalRequestService.findAllRequestsForProperty(propertyId, principal);

        return ResponseEntity.ok(requests);
    }

    /**
     * Get all rental requests made by a given tenant
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<RentalRequestDto>> getRequestsForTenant(
            @PathVariable Long tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<RentalRequestDto> requests =
                rentalRequestService.findAllRequestsForTenant(tenantId,principal);

        return ResponseEntity.ok(requests);
    }

    /**
     * GET /api/v1/rental-requests/{id}
     * Récupère une demande par ID.
     * * The only one who can come here is the Admin (ROLE_ADMIN)
     */
    @GetMapping("/{id}")
    public ResponseEntity<RentalRequestDto> getRequestById(@PathVariable Long id) {
        RentalRequestDto request = rentalRequestService.getRequestById(id);
        return ResponseEntity.ok(request);
    }
    /**
     * GET /api/v1/rental-requests/
     * Récupère tous les de;qndes
     */
    @GetMapping
    public ResponseEntity<List<RentalRequestDto>> getAllRequests(@AuthenticationPrincipal UserPrincipal principal) {
        List<RentalRequestDto> request = rentalRequestService.getAllRequests(principal);
        return ResponseEntity.ok(request);
    }

    /**
     * PUT /api/v1/rental-requests/{id}/status
     * Étape 2: Met à jour le statut (ACCEPTED, REJECTED, EXPIRED).
     * Landloard qui vas faire ca
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<RentalRequestDto> updateRequestStatus(
            @PathVariable Long id,
            @RequestBody RentalRequestStatusUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        RentalRequestDto updatedRequest = rentalRequestService.updateRequestStatus(id, dto, principal);
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * DELETE /api/v1/rental-requests/{id}
     * Supprime une demande.
     * Autorisation : Tout utilisateur authentifié (Tenant ou Landlord dans la logique métier du service).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRentalRequest(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        rentalRequestService.deleteRequest(id, principal);
        return ResponseEntity.noContent().build();
    }
}
