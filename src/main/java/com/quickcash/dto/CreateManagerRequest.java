
package com.quickcash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateManagerRequest(

        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?:\\+94|0)\\d{9}$",
                message = "Enter a valid Sri Lankan phone number"
        )
        String phone,

        @NotBlank
        @Size(min = 12, max = 72)
        String password

) {
}
