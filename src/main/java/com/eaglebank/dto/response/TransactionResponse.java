package com.eaglebank.dto.response;

import com.eaglebank.dto.request.CreateTransactionRequest;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TransactionResponse {
    
    private String id;
    private Double amount;
    private BankAccountResponse.Currency currency;
    private CreateTransactionRequest.TransactionType type;
    private String reference;
    private String userId;
    private OffsetDateTime createdTimestamp;

}
