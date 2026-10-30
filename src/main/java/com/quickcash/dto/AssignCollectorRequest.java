
package com.quickcash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignCollectorRequest(
        @NotNull
        @Positive
        Long collectorUserId
) {
}
