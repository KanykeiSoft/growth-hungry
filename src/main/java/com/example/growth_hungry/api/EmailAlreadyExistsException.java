package com.example.growth_hungry.api;

public class EmailAlreadyExistsException extends RuntimeException{
    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }
}
