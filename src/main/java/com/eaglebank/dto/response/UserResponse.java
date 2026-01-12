package com.eaglebank.dto.response;

import com.eaglebank.dto.Address;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserResponse {
    
    private String id;
    private String name;
    private Address address;
    private String phoneNumber;
    private String email;
    private OffsetDateTime createdTimestamp;
    private OffsetDateTime updatedTimestamp;
    private String username;

}
