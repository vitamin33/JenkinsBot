package com.epam.facts.command;

import android.content.Context;
import android.util.Log;

import com.epam.facts.model.Job;
import com.epam.facts.model.Message;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import ai.api.model.Result;

public class ShowJobsCommand extends DefaultCommand {

    private static final String TAG = ShowJobsCommand.class.getSimpleName();

    @Override
    public Message handleAiResult(Context context, Result result, StringBuilder speech) {

        Message message = super.handleAiResult(context, result, speech);

        final Map<String, JsonElement> data = result.getFulfillment().getData();
        Log.i(TAG, "Fulfillment data: " + data);

        if (data != null) {

            Type listType = new TypeToken<List<Job>>() { }.getType();
            List<Job> jobs = gson.fromJson(data.get("jobs"), listType);

            for (int i = 0; i < jobs.size(); i++) {
                speech.append(" ").append(jobs.get(i).name);
                if (i < jobs.size() - 1) {
                    speech.append(",");
                }
            }
            message.setJobs(jobs);
        }
        return message;
    }
}
