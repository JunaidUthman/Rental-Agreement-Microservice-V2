package com.lsiproject.app.rentalagreementmicroservicev2.services;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PaymentCreationDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PaymentDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PaymentStatusDto;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PropertyResponseDTO;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.Payment;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.RentalContract;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.PaymentStatus;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.RentalContractState;
import com.lsiproject.app.rentalagreementmicroservicev2.mappers.PaymentMapper;
import com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients.PropertyMicroService;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.PaymentRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.RentalContractRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.security.UserPrincipal;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.lsiproject.app.rentalagreementmicroservicev2.enums.RentalContractState.ACTIVE;

/**
 * Service pour la gestion de l'historique des paiements.
 * Les opérations de CREATE sont destinées au service d'écoute blockchain.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalContractRepository contractRepository;
    private final PaymentMapper paymentMapper;
    private final PropertyMicroService propertyMicroService;

    public PaymentService(
            PropertyMicroService propertyMicroService,
            PaymentRepository paymentRepository,
            RentalContractRepository contractRepository,
            PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.paymentMapper = paymentMapper;
        this.propertyMicroService = propertyMicroService;
    }

    // --- CREATE Operation (Déclenché par l'événement blockchain RentPaid) ---

    /**
     * Enregistre un nouveau paiement. Typiquement appelé par un mécanisme de synchronisation blockchain.
     * @param dto Les détails du paiement (incluant le txHash).
     * @return Le DTO du paiement enregistré.
     */
    @Transactional
    public PaymentDto createPayment(PaymentCreationDto dto) {
        // 1. Vérification d'unicité (Audit)
        if (paymentRepository.existsByTxHash(dto.getTxHash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment with transaction hash " + dto.getTxHash() + " already exists.");
        }

        // 2. Vérification de l'existence du contrat
        RentalContract contract = contractRepository.findById(dto.getRentalContractId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental contract not found."));


        if(contract.getState() != ACTIVE){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment rejected: The contract is not in an ACTIVE state. Current state: " + contract.getState());
        }

        if(contract.getPayedAmount() >= contract.getTotalAmountToPay()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment rejected: The contract is already fully paid.");
        }
        else{
            // 3. Création de l'entité
            Payment payment = new Payment();
            payment.setRentalContract(contract);
            payment.setAmount(dto.getAmount());
            payment.setTxHash(dto.getTxHash());
            payment.setStatus(dto.getStatus() != null ? dto.getStatus() : PaymentStatus.CONFIRMED); // Par défaut CONFIRMED
            payment.setTimestamp(dto.getTimestamp());
            payment.setTenantId(dto.getTenantId());

            contract.setPayedAmount(contract.getPayedAmount()+ dto.getAmount());
            contractRepository.save(contract);

            // 4. Sauvegarde
            payment = paymentRepository.save(payment);
            return paymentMapper.toDto(payment);
        }

    }

    // --- READ Operations ---

    /**
     * Récupère un paiement par son ID, avec vérification d'autorisation.
     */
    public PaymentDto getPaymentById(Long paymentId, UserPrincipal principal) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));

        // Vérification d'autorisation: Seul le locataire ou le propriétaire du contrat peut voir ce paiement
        RentalContract contract = payment.getRentalContract();
        if (!Objects.equals(contract.getTenantId(), principal.getIdUser()) &&
                !Objects.equals(contract.getOwnerId(), principal.getIdUser())&&
                !principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("User is not authorized to view this payment.");
        }

        return paymentMapper.toDto(payment);
    }

    /**
     * Récupère l'historique de paiement pour un contrat spécifique, avec vérification d'autorisation.
     */
    public List<PaymentDto> getPaymentHistoryByContract(Long contractId, UserPrincipal principal) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental contract not found."));

        // Vérification d'autorisation: Seul le locataire ou le propriétaire du contrat et ladmin peut voir cet historique
        if (!Objects.equals(contract.getTenantId(), principal.getIdUser()) &&
                !Objects.equals(contract.getOwnerId(), principal.getIdUser())&&
                !principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("User is not authorized to view this payment history.");
        }

        return paymentMapper.toDtoList(contract.getPaymentHistory());
    }



}