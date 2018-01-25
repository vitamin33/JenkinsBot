package com.epam.facts.command;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.epam.facts.model.Artifact;
import com.epam.facts.model.Download;
import com.epam.facts.model.Message;
import com.epam.facts.service.DownloadService;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

import ai.api.model.Result;

public class DownloadBuildCommand extends DefaultCommand {

    private static final String TAG = DownloadBuildCommand.class.getSimpleName();

    @Override
    public Message handleAiResult(Context context, Result result, StringBuilder speech) {

        Message message = super.handleAiResult(context, result, speech);

        final Map<String, JsonElement> data = result.getFulfillment().getData();
        Log.i(TAG, "Fulfillment data: " + data);

        if (data != null) {
            Type listType = new TypeToken<Artifact>() {}.getType();
            Artifact artifact = gson.fromJson(data.get("artifact"), listType);
            startDownload(context, artifact.name, artifact.url);
        }
        return message;
    }

    private void startDownload(Context context, String name, String url){

        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(Download.DOWNLOAD_URL_EXTRA, url);
        intent.putExtra(Download.DOWNLOAD_NAME_EXTRA, name);
        context.startService(intent);

    }
}
