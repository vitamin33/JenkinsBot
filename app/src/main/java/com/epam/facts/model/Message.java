package com.epam.facts.model;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {


    private String userId;
    private String message, createdAt;
    private String messageAction;
    private long id;

    private List<Job> mJobs;

    public Message() {
    }

    public Message(long id, String message, String createdAt, String userId, String messageAction) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
        this.userId = userId;
        this.messageAction = messageAction;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageAction() {
        return messageAction;
    }

    public void setMessageAction(String messageAction) {
        this.messageAction = messageAction;
    }

    public List<Job> getJobs() {
        return mJobs;
    }

    public void setJobs(List<Job> mJobs) {
        this.mJobs = mJobs;
    }
}
