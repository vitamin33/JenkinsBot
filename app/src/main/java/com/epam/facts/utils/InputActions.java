package com.epam.facts.utils;

import com.epam.facts.command.AiCommand;
import com.epam.facts.command.DefaultCommand;
import com.epam.facts.command.DownloadBuildCommand;
import com.epam.facts.command.ShowJobsCommand;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InputActions {
    public static String SHOW_JOBS = "input.show_jobs";
    public static String DOWNLOAD_BUILD = "input.download_build";
    public static String RESTART = "input.restart";
    public static String BUILD = "input.build";


    private static final Map<String, AiCommand> mInputActions;
    static {
        Map<String, AiCommand> aMap = new HashMap<>();
        aMap.put(SHOW_JOBS, new ShowJobsCommand());
        aMap.put(DOWNLOAD_BUILD, new DownloadBuildCommand());
        mInputActions = Collections.unmodifiableMap(aMap);
    }

    public static AiCommand createAiCommand(String inputAction) {
        AiCommand command = mInputActions.get(inputAction);

        return command != null ? command : new DefaultCommand();
    }
}
