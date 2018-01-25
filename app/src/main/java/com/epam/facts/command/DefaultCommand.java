package com.epam.facts.command;


import android.content.Context;

import com.epam.facts.adapter.ChatRoomThreadAdapter;
import com.epam.facts.model.Message;
import com.google.gson.Gson;

import ai.api.model.Result;

public class DefaultCommand implements AiCommand {

    protected Gson gson = new Gson();

    @Override
    public Message handleAiResult(Context context, Result result, StringBuilder speech) {
        Message message = new Message();

        long timeMillis = System.currentTimeMillis();
        message.setId(timeMillis);
        message.setMessage(result.getFulfillment().getSpeech());
        message.setCreatedAt(String.valueOf(timeMillis));
        message.setUserId(ChatRoomThreadAdapter.BOT_ID);
        message.setMessageAction(result.getAction());

        return message;
    }
}
