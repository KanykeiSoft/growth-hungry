package com.example.growth_hungry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegistrationDto {

    @NotBlank(message = "Username must not be empty")
    @Size(min = 3, max = 50, message = "Username must be 3..50 chars")
    private String username;

    @NotBlank(message = "Password must not be empty")
    @Size(min = 6, max = 100, message = "Password must be at least 6 chars")
    private String password;

    @NotBlank(message = "Email must not be empty")
    @Email(message = "Email is invalid")
    private String email;

    public UserRegistrationDto() {
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
