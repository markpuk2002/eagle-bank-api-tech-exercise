package com.eaglebank.dto.response;

import com.eaglebank.dto.request.CreateBankAccountRequest;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BankAccountResponse {
    
    private String accountNumber;
    private String sortCode;
    private String name;
    private CreateBankAccountRequest.AccountType accountType;
    private Double balance;
    private Currency currency;
    private OffsetDateTime createdTimestamp;
    private OffsetDateTime updatedTimestamp;

    public enum Currency {
        GBP
    }
}
