package com.epam.facts.model;


import ai.api.model.AIError;
import ai.api.model.AIResponse;

public class AiResponse {

    private AIResponse mResponse;
    private AIError mError;

    public AiResponse(AIResponse response) {
        mResponse = response;
    }

    public AiResponse(AIError error) {
        mError = error;
    }

    public boolean hasError() {
        return mError != null;
    }

    public AIResponse getResponse() {
        return mResponse;
    }

    public AIError getError() {
        return mError;
    }
}
