package com.quickcash.dto;

import com.quickcash.entity.FollowUpOutcome;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateLoanFollowUpRequest(

        @NotNull
        FollowUpOutcome outcome,

        @NotBlank
        @Size(max = 500)
        String note,

        LocalDate nextFollowUpDate

) {
}