package com.eaglebank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Address {
    
    @NotBlank(message = "Line1 is required")
    @Size(max = 100, message = "Line1 must not exceed 100 characters")
    private String line1;
    
    @Size(max = 100, message = "Line2 must not exceed 100 characters")
    private String line2;
    
    @Size(max = 100, message = "Line3 must not exceed 100 characters")
    private String line3;
    
    @NotBlank(message = "Town is required")
    @Size(max = 100, message = "Town must not exceed 100 characters")
    private String town;
    
    @NotBlank(message = "County is required")
    @Size(max = 50, message = "County must not exceed 50 characters")
    private String county;
    
    @NotBlank(message = "Postcode is required")
    @Size(max = 20, message = "Postcode must not exceed 20 characters")
    private String postcode;

}
