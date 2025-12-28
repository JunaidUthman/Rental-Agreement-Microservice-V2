package com.lsiproject.app.rentalagreementmicroservicev2.services;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.KeyDeliveryUpdateDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PropertyResponseDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.RentalContractCreationDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.RentalContractDto;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.Payment;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.RentalContract;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.PaymentStatus;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.RentalContractState;
import com.lsiproject.app.rentalagreementmicroservicev2.mappers.RentalContractMapper;
import com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients.PropertyMicroService;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.PaymentRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.RentalContractRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.security.UserPrincipal;
import feign.FeignException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour la gestion des contrats de location (RentalContract) en BDD.
 * L'interaction avec la blockchain est intentionnellement omise à ce stade.
 */
@Service
public class RentalContractService {

    private final RentalContractRepository contractRepository;
    private final RentalContractMapper contractMapper;
    private final PropertyMicroService propertyMicroService;
    private final PaymentRepository  paymentRepository;

    public RentalContractService(
            PaymentRepository  paymentRepository,
            RentalContractRepository contractRepository,
            PropertyMicroService propertyMicroService,
            RentalContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.contractMapper = contractMapper;
        this.propertyMicroService = propertyMicroService;
        this.paymentRepository = paymentRepository;
    }

    // =========================================================================================
    // READ Operations
    // =========================================================================================

    /**
     * Récupère un contrat par son ID interne.
     */
    public RentalContractDto getContractById(Long contractId,UserPrincipal principal) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental contract not found."));

        if(!principal.getIdUser().equals(contract.getTenantId()) &&
                !principal.getIdUser().equals(contract.getOwnerId()) &&
                !principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) )
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"the only one who can get the contract is the tenet,owner or the admin");

        }


        return contractMapper.toDto(contract);
    }

    /**
     * Récupère tous les contrats d'un utilisateur (en tant que propriétaire ou locataire).
     */
    public List<RentalContractDto> getAllContractsForUser(UserPrincipal principal) {

        // Trouver les contrats où l'utilisateur est propriétaire
        List<RentalContract> ownerContracts = contractRepository.findByOwnerId(principal.getIdUser());
        // Trouver les contrats où l'utilisateur est locataire
        List<RentalContract> tenantContracts = contractRepository.findByTenantId(principal.getIdUser());

        ownerContracts.addAll(tenantContracts);

        return contractMapper.toDtoList(ownerContracts);
    }

    // =========================================================================================
    // CREATE Operation (Déclenché par le paiement initial)
    // =========================================================================================

    /**
     * Crée un nouveau contrat dans la BDD après un paiement initial théorique (Étape 3).
     * Le statut initial est PENDING_RESERVATION.
     * @param dto Les termes du contrat.
     * @param principal L'utilisateur authentifié (Tenant).
     * @return Le contrat créé en BDD.
     */
    @Transactional
    public RentalContractDto createContract(RentalContractCreationDto dto, UserPrincipal principal) {

        PropertyResponseDTO property;

        try {
            property = propertyMicroService.getPropertyById(dto.getPropertyId());
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }

        // 1. Création de l'entité BDD
        RentalContract contract = new RentalContract();

        contract.setAgreementIdOnChain(dto.getAgreementIdOnChain());

        // Données du contrat
        contract.setOwnerId(dto.getOwnerId());
        contract.setTenantId(principal.getIdUser());
        contract.setPropertyId(dto.getPropertyId());

        contract.setSecurityDeposit(dto.getSecurityDeposit());
        contract.setRentAmount(dto.getRentAmount());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());

        String rentalType = property.typeOfRental().toString();

        Double TotalAmountToPay = contract.calculateTotalAmount(contract.getStartDate(), contract.getEndDate(), contract.getRentAmount(),rentalType );

        contract.setTotalAmountToPay(TotalAmountToPay);
        contract.setPayedAmount(contract.getRentAmount());



        // Statuts initiaux requis
        contract.setIsKeyDelivered(false);
        contract.setIsPaymentReleased(false);
        contract.setState(RentalContractState.PENDING_RESERVATION);

        // 2. Sauvegarde
        contract = contractRepository.save(contract);

        Payment payment = new Payment();
        payment.setRentalContract(contract);
        payment.setAmount(contract.getRentAmount());
        payment.setTxHash(dto.getInitialTxHash());
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setTimestamp(LocalDateTime.now());

        payment.setTenantId(contract.getTenantId());

        paymentRepository.save(payment);


        return contractMapper.toDto(contract);
    }

    // =========================================================================================
    // UPDATE Operation (Confirmation de clé)
    // =========================================================================================

    /**
     * Met à jour le statut du contrat pour confirmer la remise de clé (Étape 4).
     * Cette fonction met à jour l'état dans la BDD sans interagir avec la blockchain.
     * @param contractId L'ID du contrat interne.
     * @param dto Confirmation de clé.
     * @param principal L'utilisateur authentifié (Tenant).
     * @return Le contrat mis à jour.
     */
    @Transactional
    public RentalContractDto updateKeyDeliveryStatus(Long contractId, KeyDeliveryUpdateDto dto, UserPrincipal principal) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental contract not found."));


        System.out.println("the key boolean equals =============="+dto.getIsKeyDelivered());
        // 1. Vérification d'autorisation et de statut
        if (!contract.getTenantId().equals(principal.getIdUser())) {
            throw new AccessDeniedException("Only the tenant is authorized to confirm key delivery.");
        }
        if (contract.getState() != RentalContractState.PENDING_RESERVATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract is not in PENDING_RESERVATION status.");
        }
        if (contract.getIsKeyDelivered()) {
            return contractMapper.toDto(contract); // Déjà livré, rien à faire
        }

        // 2. Mise à jour de l'état de la clé dans la BDD
        contract.setIsKeyDelivered(dto.getIsKeyDelivered());

        // 3. Mise à jour des statuts (simulant la conséquence de l'activation)
        if (dto.getIsKeyDelivered()) {
            contract.setState(RentalContractState.ACTIVE);
            contract.setIsPaymentReleased(true); // Le premier loyer est censé être libéré

        } else {
            // Si la livraison de clé est annulée (bien que peu probable dans ce flux)
            contract.setState(RentalContractState.PENDING_RESERVATION);
        }

        // 4. Sauvegarde et retour
        contract = contractRepository.save(contract);
        return contractMapper.toDto(contract);
    }

    /**
     * Termine le contrat par dispute.
     * Accessible uniquement par le locataire ou le propriétaire.
     * @param contractId L'ID du contrat interne.
     * @param principal L'utilisateur authentifié.
     * @return Le contrat mis à jour avec le statut DISPUTED.
     */
    @Transactional
    public RentalContractDto terminateContractByDispute(Long contractId, UserPrincipal principal) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental contract not found."));

        PropertyResponseDTO property;

        try {
            property = propertyMicroService.getPropertyById(contract.getPropertyId());
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }

        // 1. Vérification d'autorisation (Tenant ou Owner uniquement)
        boolean isTenant = contract.getTenantId().equals(principal.getIdUser());
        boolean isOwner = contract.getOwnerId().equals(principal.getIdUser());

        if (!isTenant && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tenant or the owner can terminate the contract by dispute.");
        }

        // 2. Vérification optionnelle de l'état actuel (par exemple, éviter de disputer un contrat déjà terminé)
        // Vous pouvez commenter cette partie si vous souhaitez autoriser la dispute quel que soit l'état actuel.
        if (contract.getState() == RentalContractState.DISPUTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract is already disputed.");
        }

        // 3. Mise à jour de l'état
        contract.setState(RentalContractState.DISPUTED);

        // 4. Sauvegarde
        contract = contractRepository.save(contract);

        propertyMicroService.updateAvailabilityToTrue(property.idProperty());

        return contractMapper.toDto(contract);
    }
}