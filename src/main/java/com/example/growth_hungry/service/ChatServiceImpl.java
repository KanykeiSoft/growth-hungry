package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
@Service
public class ChatServiceImpl implements ChatService{

    // ADD inside class:
    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    final AiClient aiClient;

    public ChatServiceImpl(AiClient aiClient) {
        this.aiClient = aiClient;
    }


    @Override
    public ChatResponse chat(ChatRequest req) {
        if(req == null){
            return new ChatResponse("Error: empty request");
        }
        if(req.getMessage() == null || req.getMessage().isBlank()){
            return new ChatResponse("Error: message must not be blank");
        }
        final String message = req.getMessage().trim();
        final String systemPrompt = (req.getSystemPrompt() == null || req.getSystemPrompt().isBlank())
                ? null : req.getSystemPrompt();
        final String model = (req.getModel() == null || req.getModel().isBlank())
                ? null : req.getModel();
        try {
            String answer = aiClient.generate(message, systemPrompt, model);

            if (answer == null || answer.isBlank()) {
                answer = "(Empty response)";

            }
            ChatResponse resp = new ChatResponse();
            resp.setReply(answer);
            resp.setContextId(req.getContextId());
            resp.setModel(model);
            return resp;
        }catch (IllegalArgumentException badInput){
            log.warn("AI request error: {}", badInput.getMessage());
            return new ChatResponse("Error calling AI: " + badInput.getMessage());
        } catch (Exception ex) {
            log.error("AI invocation error", ex);
            return new ChatResponse("Error calling AI: " + ex.getMessage());
        }
        }
    }

