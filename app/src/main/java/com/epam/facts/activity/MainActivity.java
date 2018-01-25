package com.epam.facts.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.epam.facts.SettingsManager;
import com.epam.facts.command.AiCommand;
import com.epam.facts.model.AiResponse;
import com.epam.facts.R;
import com.epam.facts.adapter.ChatRoomThreadAdapter;
import com.epam.facts.model.Message;
import com.epam.facts.utils.Config;
import com.epam.facts.utils.InputActions;
import com.epam.facts.utils.TTS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import ai.api.AIServiceException;
import ai.api.PartialResultsListener;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.ui.AIButton;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AIButton.AIButtonListener, PartialResultsListener {

    public static final String TAG = MainActivity.class.getName();

    private EditText queryEditText;

    private AIButton aiButton;
    private AIDataService aiDataService;

    private RecyclerView recyclerView;
    private ArrayList<Message> messageArrayList;
    private ChatRoomThreadAdapter mAdapter;
    private TTS textToSpeech;

    private SettingsManager mSettingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textToSpeech = new TTS();

        mSettingsManager = new SettingsManager(this);

        queryEditText = findViewById(R.id.textQuery);
        aiButton = findViewById(R.id.micButton);
        aiButton.setPartialResultsListener(this);

        findViewById(R.id.buttonSend).setOnClickListener(this);
        findViewById(R.id.buttonClear).setOnClickListener(this);

        initService();

        initRecycleView();
    }

    private void initRecycleView() {
        recyclerView = findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatRoomThreadAdapter(this, messageArrayList, ChatRoomThreadAdapter.SELF_ID);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        textToSpeech.init(getApplicationContext());
    }

    @Override
    protected void onStop() {
        super.onStop();
        textToSpeech.shutdown();
    }

    private void initService() {
        final AIConfiguration config = new AIConfiguration(Config.ACCESS_TOKEN,
                ai.api.AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiDataService = new AIDataService(this, config);

        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiButton.initialize(config);
        aiButton.setResultsListener(this);
    }


    private void clearEditText() {
        queryEditText.setText("");
    }

    private void sendAiRequest() {

        final String queryString = String.valueOf(queryEditText.getText());

        if (TextUtils.isEmpty(queryString)) {
            onAiError(new AIError(getString(R.string.non_empty_query)));
            return;
        }

        addMessageForBot(queryString);

        Single.defer(doAiRequest(queryString))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(getAiSingleObserver());
    }

    private Callable<? extends SingleSource<AiResponse>> doAiRequest(final String query) {
        return (Callable<SingleSource<AiResponse>>) () -> {
            final AIRequest request = new AIRequest();

            if (!TextUtils.isEmpty(query))
                request.setQuery(query);

            final List<AIContext> contexts = Collections.singletonList(createServerInfoContext());
            RequestExtras requestExtras = new RequestExtras(contexts, null);


            try {
                return Single.just(new AiResponse(aiDataService.request(request, requestExtras)));
            } catch (final AIServiceException e) {
                AIError aiError = new AIError(e);
                return Single.just(new AiResponse(aiError));
            }
        };
    }

    private AIContext createServerInfoContext() {

        AIContext context = new AIContext(Config.SERVER_CONTEXT_NAME);

        Map<String, String> params = new HashMap<>();
        params.put(Config.HOST_PARAMETER_KEY, mSettingsManager.getServerUrl());
        params.put(Config.USER_PARAMETER_KEY, mSettingsManager.getUser());
        params.put(Config.PASSWORD_PARAMETER_KEY, mSettingsManager.getPassword());

        context.setParameters(params);

        return context;
    }


    private SingleObserver<AiResponse> getAiSingleObserver() {
        return new SingleObserver<AiResponse>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, " onSubscribe : " + d.isDisposed());
            }

            @Override
            public void onSuccess(AiResponse aiResponse) {
                Log.d(TAG, " onNext value : " + aiResponse);

                if (aiResponse.hasError()) {
                    onAiError(aiResponse.getError());
                } else {
                    onAiResult(aiResponse.getResponse());
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, " onError : " + e.getMessage());
            }
        };
    }


    private void onAiResult(final AIResponse response) {

        Log.d(TAG, "onResult");

        Log.i(TAG, "Received success response");

        // this is example how to get different parts of result object
        final Status status = response.getStatus();
        Log.i(TAG, "Status code: " + status.getCode());
        Log.i(TAG, "Status type: " + status.getErrorType());

        final Result result = response.getResult();
        Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

        Log.i(TAG, "Action: " + result.getAction());

        addResponseToUser(result);
    }

    private void addResponseToUser(Result result) {

        AiCommand command = InputActions.createAiCommand(result.getAction());

        StringBuilder speech = new StringBuilder(result.getFulfillment().getSpeech());

        Message message = command.handleAiResult(this, result, speech);

        Log.i(TAG, "Speech: " + speech);

        textToSpeech.speak(speech.toString());

        addAnswerFromBot(message);
    }

    private void addAnswerFromBot(Message message) {

        messageArrayList.add(message);

        updateAdapter();
    }

    private void addErrorAnswerFromBot(AIError error) {

        long timeMillis = System.currentTimeMillis();

        Message message = new Message();
        message.setId(timeMillis);
        message.setMessage(error.toString());
        message.setCreatedAt(String.valueOf(timeMillis));
        message.setUserId(ChatRoomThreadAdapter.BOT_ID);
        messageArrayList.add(message);

        updateAdapter();
    }

    private void addMessageForBot(String queryString) {

        long timeMillis = System.currentTimeMillis();

        Message message = new Message();
        message.setId(timeMillis);
        message.setMessage(queryString);
        message.setCreatedAt(String.valueOf(timeMillis));
        message.setUserId(ChatRoomThreadAdapter.SELF_ID);
        messageArrayList.add(message);

        updateAdapter();
    }

    private void addUserMessage(String queryString) {

        long timeMillis = System.currentTimeMillis();

        Message message = new Message();
        message.setId(timeMillis);
        message.setMessage(queryString);
        message.setCreatedAt(String.valueOf(timeMillis));
        message.setUserId(ChatRoomThreadAdapter.SELF_ID);
        messageArrayList.add(message);

        updateAdapter();
    }

    private void updateAdapter() {
        mAdapter.notifyDataSetChanged();
        if (mAdapter.getItemCount() > 1) {
            // scrolling to bottom of the recycler view
            recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
        }
    }

    private void onAiError(final AIError error) {
        runOnUiThread(() -> addErrorAnswerFromBot(error));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonClear:
                clearEditText();
                break;
            case R.id.buttonSend:
                sendAiRequest();
                break;
        }
    }

    @Override
    public void onResult(AIResponse aiResponse) {
        onAiResult(aiResponse);
    }

    @Override
    public void onError(AIError error) {
        onAiError(error);
    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void onPartialResults(List<String> partialResults, boolean partial) {
        if (!partial) {
            addUserMessage(partialResults.get(0));
            Log.d(TAG, "partial results: " + partialResults.get(0));
        }
    }
}
