
package com.quickcash.dto;

import com.quickcash.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateManagerStatusRequest(
        @NotNull UserStatus status
) {
}

