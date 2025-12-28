package com.lsiproject.app.rentalagreementmicroservicev2.controllers;

import com.lsiproject.app.rentalagreementmicroservicev2.entities.DisputeSummary;
import com.lsiproject.app.rentalagreementmicroservicev2.services.DisputeSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
public class DisputeSummaryController {

    private final DisputeSummaryService disputeSummaryService;

    public DisputeSummaryController(DisputeSummaryService disputeSummaryService) {
        this.disputeSummaryService = disputeSummaryService;
    }

    /**
     * Endpoint to retrieve all dispute summaries.
     * Useful for AI training or Admin dashboards.
     * GET /api/disputes
     */
    @GetMapping
    public ResponseEntity<List<DisputeSummary>> getAllDisputes() {
        List<DisputeSummary> disputes = disputeSummaryService.getAllDisputeSummaries();
        return ResponseEntity.ok(disputes);
    }
}