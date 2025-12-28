package com.lsiproject.app.rentalagreementmicroservicev2.controllers;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.PaymentStatusDto;
import com.lsiproject.app.rentalagreementmicroservicev2.entities.PaymentReport;
import com.lsiproject.app.rentalagreementmicroservicev2.services.PaymentReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PaymentReportService reportService;

    public ReportController(PaymentReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Endpoint 1: Create a new report for a specific contract
     * POST /api/reports/generate/{contractId}
     */
    @PostMapping("/generate/{contractId}")
    public ResponseEntity<PaymentStatusDto> createReport(@PathVariable Long contractId) {
        PaymentStatusDto reportDto = reportService.generateAndSaveReport(contractId);
        return ResponseEntity.ok(reportDto);
    }

    /**
     * Endpoint 2: Get all reports stored in the database
     * GET /api/reports
     */
    @GetMapping
    public ResponseEntity<List<PaymentReport>> getAllReports() {
        List<PaymentReport> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }
}