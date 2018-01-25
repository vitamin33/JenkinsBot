package com.epam.facts.command;

import android.content.Context;

import com.epam.facts.model.Message;

import ai.api.model.Result;


public interface AiCommand {
    Message handleAiResult(Context context, Result result, StringBuilder speech);
}
