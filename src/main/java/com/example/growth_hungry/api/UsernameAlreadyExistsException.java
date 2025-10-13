package com.example.growth_hungry.api;

public class UsernameAlreadyExistsException extends RuntimeException{
    public UsernameAlreadyExistsException(String u) {
        super("Username '"+u+"' already exists");
    }

}
