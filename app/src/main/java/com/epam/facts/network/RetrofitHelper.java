package com.epam.facts.network;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitHelper {

    public JenkinsService getJenkinsService(String user, String password) {
        final Retrofit retrofit = createRetrofit(user, password);
        return retrofit.create(JenkinsService.class);
    }

    private OkHttpClient createOkHttpClient(String user, String password) {
        final OkHttpClient.Builder httpClient =
                new OkHttpClient.Builder();
        httpClient.addInterceptor(new BasicAuthInterceptor(user, password));

        return httpClient.build();
    }

    /**
     * Creates a pre configured Retrofit instance
     */
    private Retrofit createRetrofit(String user, String password) {
        return new Retrofit.Builder()
                .baseUrl("http://your.jenkins.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // <- add this
                .client(createOkHttpClient(user, password))
                .build();
    }
}
