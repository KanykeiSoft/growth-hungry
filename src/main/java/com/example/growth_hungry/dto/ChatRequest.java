package com.example.growth_hungry.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;


public class ChatRequest {

    @NotBlank(message = "message must not be blank")
    @Size(max = 4000, message = "message is too long")
    private String message;

    @Size(max = 100, message = "contextId is too long")
    private String contextId;

    @Size(max = 2000, message = "systemPrompt is too long")
    private String systemPrompt;


    @Size(max = 100, message = "model is too long")
    private String model;

    public ChatRequest(String message) {
        this.message = message;
    }

    public ChatRequest() { }

    public ChatRequest(String message, String contextId, String systemPrompt, String model) {
        this.message = message;
        this.contextId = contextId;
        this.systemPrompt = systemPrompt;
        this.model = model;
    }



    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }


    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }


}

