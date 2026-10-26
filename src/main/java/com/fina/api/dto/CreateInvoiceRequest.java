package com.fina.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvoiceRequest(
        @NotBlank String accountId,
        @NotBlank @Size(max = 120) String customerName,
        @NotBlank @Email @Size(max = 180) String customerEmail,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull @FutureOrPresent LocalDate dueOn
) {
}
