package com.eaglebank.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ListBankAccountsResponse {
    
    private List<BankAccountResponse> accounts;

}
