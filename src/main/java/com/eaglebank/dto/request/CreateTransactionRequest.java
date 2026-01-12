package com.eaglebank.dto.request;

import com.eaglebank.dto.response.BankAccountResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTransactionRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", message = "Amount must be at least 0.00")
    @DecimalMax(value = "10000.00", message = "Amount must be at most 10000.00")
    private Double amount;
    
    @NotNull(message = "Currency is required")
    private BankAccountResponse.Currency currency;
    
    @NotNull(message = "Type is required")
    private TransactionType type;
    
    @Size(max = 255, message = "Reference must not exceed 255 characters")
    private String reference;

    public enum TransactionType {
        deposit, withdrawal
    }
}
