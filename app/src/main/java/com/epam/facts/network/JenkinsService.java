package com.epam.facts.network;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface JenkinsService {

    @GET
    @Streaming
    Call<ResponseBody> downloadFile(@Url String url);

    @GET
    Observable<JenkinsUser> getUserInfo(@Url String url);
}
