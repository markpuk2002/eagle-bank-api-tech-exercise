package com.eaglebank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @Column(name = "name", nullable = true, length = 255)
    private String name;
    
    @Embedded
    private AddressEntity address;
    
    @Column(name = "phone_number", nullable = true, length = 20)
    private String phoneNumber;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private OffsetDateTime createdTimestamp;
    
    @Column(name = "updated_timestamp", nullable = false)
    private OffsetDateTime updatedTimestamp;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    public User() {
    }
    
    public User(String id, String name, AddressEntity address, String phoneNumber, String email, String username, String password) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.createdTimestamp = OffsetDateTime.now();
        this.updatedTimestamp = OffsetDateTime.now();
        this.username = username;
        this.password = password;
    }
    
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdTimestamp == null) {
            createdTimestamp = OffsetDateTime.now();
        }
        if (updatedTimestamp == null) {
            updatedTimestamp = OffsetDateTime.now();
        }
    }
    
    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedTimestamp = OffsetDateTime.now();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public AddressEntity getAddress() {
        return address;
    }
    
    public void setAddress(AddressEntity address) {
        this.address = address;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public OffsetDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(OffsetDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public OffsetDateTime getUpdatedTimestamp() {
        return updatedTimestamp;
    }
    
    public void setUpdatedTimestamp(OffsetDateTime updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}