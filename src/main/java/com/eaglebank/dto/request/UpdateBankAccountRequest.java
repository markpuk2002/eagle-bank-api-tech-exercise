package com.eaglebank.dto.request;

import com.eaglebank.dto.request.CreateBankAccountRequest.AccountType;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBankAccountRequest {
    
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    private AccountType accountType;

}
