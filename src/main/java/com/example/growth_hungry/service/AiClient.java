package com.example.growth_hungry.service;

public interface AiClient {

    /**
     * Sends user input to the AI model and returns generated response.
     *
     * @param message      user input, must not be blank
     * @param systemPrompt optional system instruction
     * @param model        model name, may be null to use default
     * @return model response text
     * @throws IllegalArgumentException if message is null or blank
     */
    String generate(String message, String systemPrompt, String model);
}
