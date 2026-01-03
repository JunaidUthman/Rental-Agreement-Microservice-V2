package com.lsiproject.app.rentalagreementmicroservicev2.services;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PaymentStatusDto;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.PaymentReport;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.RentalContract;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.PaymentStatus;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.RentalContractState;
import com.lsiproject.app.rentalagreementmicroservicev2.openFeignClients.PropertyMicroService;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.PaymentReportRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.PaymentRepository;
import com.lsiproject.app.rentalagreementmicroservicev2.repositories.RentalContractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentReportService {

    private final RentalContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentReportRepository reportRepository;
    private final PropertyMicroService propertyMicroService;

    public PaymentReportService(
            RentalContractRepository contractRepository,
            PaymentRepository paymentRepository,
            PaymentReportRepository reportRepository,
            PropertyMicroService propertyMicroService) {
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.reportRepository = reportRepository;
        this.propertyMicroService = propertyMicroService;
    }

    /**
     * Generates a new report, saves it to DB, and returns the DTO.
     */
    @Transactional
    public PaymentStatusDto generateAndSaveReport(Long contractId) {
        // 1. Fetch Contract
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));

        // 2. Fetch Total Paid (Only CONFIRMED payments)
        Double totalPaid = paymentRepository.sumAmountByContractId(contract.getIdContract());
        System.out.println("Total paid : " + totalPaid);
        System.out.println(LocalDateTime.now());
        if (totalPaid == null) totalPaid = 0.0;

        // 3. Get Rental Type (Daily/Monthly) from Microservice
        String rentalType = "MONTHLY"; // Default
        try {
            rentalType = propertyMicroService.getTypeOfRental(contract.getPropertyId()).toString();
        } catch (Exception e) {
            System.err.println("Could not fetch rental type, defaulting to MONTHLY: " + e.getMessage());
        }

        // 4. Waterfall Logic Setup
        LocalDate current = contract.getStartDate();
        LocalDate today = LocalDate.now();
        Double rent = contract.getRentAmount();

        int paidPeriods = 0;
        int missedPeriods = 0;
        List<LocalDate> missedDatesList = new ArrayList<>();
        Double moneyPool = totalPaid;
        Double totalExpected = 0.0;

        // 5. The Loop (Stops at Today or Contract End)
        while (!current.isAfter(today) && !current.isAfter(contract.getEndDate())) {

            totalExpected += rent;

            // Check if money covers this period (with small epsilon for float safety)
            if (moneyPool >= rent - 0.01) {
                paidPeriods++;
                moneyPool -= rent;
            } else {
                missedPeriods++;
                missedDatesList.add(current);
                moneyPool = 0.0; // Pool empty
            }

            // Move cursor
            if ("DAILY".equalsIgnoreCase(rentalType)) {
                current = current.plusDays(1);
            } else {
                current = current.plusMonths(1);
            }
        }

        // 6. Determine Status
        String status = "UP_TO_DATE";
        if (missedPeriods > 0) status = "LATE";
        if (contract.getState() == RentalContractState.DISPUTED) status = "DISPUTED";

        // 7. Save Report to Database
        PaymentReport report = reportRepository.findByRentalContract_IdContract(contractId);

        if(report==null){
            report = new PaymentReport();
        }
        report.setRentalContract(contract);
        report.setTotalPaidSoFar(totalPaid);
        report.setTotalExpectedSoFar(totalExpected);
        report.setPaidPeriods(paidPeriods);
        report.setMissedPeriods(missedPeriods);
        report.setStatus(status);
        report.setTenentID(contract.getTenantId());

        // Convert List<LocalDate> to comma-separated String
        String missedDatesString = missedDatesList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));
        report.setMissedDates(missedDatesString);

        reportRepository.save(report);

        // 8. Return DTO
        return PaymentStatusDto.builder()
                .totalPaidSoFar(totalPaid)
                .totalExpectedSoFar(totalExpected)
                .paidPeriods(paidPeriods)
                .missedPeriods(missedPeriods)
                .missedDates(missedDatesList)
                .status(status)
                .tenentId(contract.getTenantId())
                .build();
    }

    /**
     * Fetch all reports (Just for admin/debugging purposes)
     */
    public List<PaymentReport> getAllReports() {
        return reportRepository.findAll();
    }
}