
package com.quickcash.controller;

import com.quickcash.dto.CollectorBorrowerSearchResponse;
import com.quickcash.service.CollectorBorrowerSearchService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collector/borrowers")
@PreAuthorize("hasRole('COLLECTOR')")
public class CollectorBorrowerSearchController {

    private final CollectorBorrowerSearchService searchService;

    public CollectorBorrowerSearchController(
            CollectorBorrowerSearchService searchService
    ) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<CollectorBorrowerSearchResponse>>
    searchBorrowers(
            Authentication authentication,
            @RequestParam("query") String query
    ) {

        return ResponseEntity.ok(
                searchService.searchBorrowers(
                        authentication.getName(),
                        query
                )
        );
    }
}
