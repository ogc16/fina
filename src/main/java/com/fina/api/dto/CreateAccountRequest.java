package com.fina.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String ownerName,
        @NotBlank @Email @Size(max = 180) String email,
        @DecimalMin(value = "0.00") BigDecimal initialBalance
) {
}
