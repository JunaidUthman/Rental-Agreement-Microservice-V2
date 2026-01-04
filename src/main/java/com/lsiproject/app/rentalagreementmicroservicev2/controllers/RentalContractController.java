package com.lsiproject.app.rentalagreementmicroservicev2.controllers;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.KeyDeliveryUpdateDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.RentalContractCreationDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.RentalContractDto;
import com.lsiproject.app.rentalagreementmicroservicev2.security.UserPrincipal;
import com.lsiproject.app.rentalagreementmicroservicev2.services.RentalContractService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour gérer le cycle de vie des contrats de location (RentalContract).
 */
@RestController
@RequestMapping("/api/rentalAgreement-microservice/rental-contracts")
public class RentalContractController {

    private final RentalContractService contractService;

    public RentalContractController(RentalContractService contractService) {
        this.contractService = contractService;
    }



    /**
     * POST /api/v1/rental-contracts
     * Crée un contrat interne dans la BDD après que le paiement initial (Escrow) ait été initié.
     * Le statut initial est PENDING_RESERVATION.
     *
     * @param dto Les termes du contrat.
     * @param principal L'utilisateur authentifié (Tenant).
     * @return ResponseEntity avec le contrat créé.
     */
    @PostMapping
    public ResponseEntity<RentalContractDto> createContract(
            @RequestBody RentalContractCreationDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        RentalContractDto createdContract = contractService.createContract(dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdContract);
    }



    // --- DISPLAY Operations ---

    /**
     * GET /api/v1/rental-contracts/{id}
     * Récupère un contrat par ID.
     *
     * @param id L'ID du contrat interne.
     * @return ResponseEntity avec les détails du contrat.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RentalContractDto> getContractById(@PathVariable Long id,@AuthenticationPrincipal UserPrincipal principal) {

        RentalContractDto contract = contractService.getContractById(id,principal);
        return ResponseEntity.ok(contract);
    }

    /**
     * GET /api/v1/rental-contracts/user/me
     * Récupère tous les contrats de l'utilisateur authentifié (tenant et owner).
     *
     * @param principal L'utilisateur authentifié.
     * @return ResponseEntity avec la liste des contrats.
     */
    @GetMapping("/user/me")
    public ResponseEntity<List<RentalContractDto>> getAllUserContracts(@AuthenticationPrincipal UserPrincipal principal) {
        List<RentalContractDto> contracts = contractService.getAllContractsForUser(principal);
        return ResponseEntity.ok(contracts);
    }

    // --- UPDATE Operation (Étape 4: Confirmation de clé) ---

    /**
     * PUT /api/v1/rental-contracts/{id}/key-delivery
     * Met à jour la confirmation de la clé par le locataire. Déclenche l'activation du contrat en BDD.
     *
     * @param id L'ID du contrat interne.
     * @param dto Confirmation de clé.
     * @param principal L'utilisateur authentifié (Tenant).
     * @return ResponseEntity avec le contrat mis à jour.
     */
    @PutMapping("/{id}/key-delivery")
    public ResponseEntity<RentalContractDto> updateKeyDelivery(
            @PathVariable Long id,
            @RequestBody KeyDeliveryUpdateDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        RentalContractDto updatedContract = contractService.updateKeyDeliveryStatus(id, dto, principal);
        return ResponseEntity.ok(updatedContract);
    }

    @PutMapping("/{id}/dispute")
    public ResponseEntity<RentalContractDto> terminateContractByDispute(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        RentalContractDto updatedContract = contractService.terminateContractByDispute(id, principal);
        return ResponseEntity.ok(updatedContract);
    }
}