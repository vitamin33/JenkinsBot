/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.facts;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import ai.api.util.BluetoothController;

public class SettingsManager {

    private static final String SETTINGS_PREFS_NAME = "ai.api.APP_SETTINGS";
    private static final String PREF_USE_BLUETOOTH = "USE_BLUETOOTH";
    private static final String PREF_SERVER_URL = "SERVER_URL";
    private static final String PREF_USER = "USER";
    private static final String PREF_PASSWORD = "PASSWORD";

    private final Context context;
    private SharedPreferences prefs;

    private boolean useBluetooth;
    private String mServerUrl;
    private String mUser;

    /*
     *  store password in SharedPreferences is not secured, as it's PoC
     *  project and we don't care on security for now
     */
    private String mPassword;

    public SettingsManager(final Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);

        useBluetooth = prefs.getBoolean(PREF_USE_BLUETOOTH, true);

        mServerUrl = prefs.getString(PREF_SERVER_URL, "");
        mUser = prefs.getString(PREF_USER, "");

        mPassword = prefs.getString(PREF_PASSWORD, "");
    }

    public void setUseBluetooth(final boolean useBluetooth) {
        this.useBluetooth = useBluetooth;

        prefs.edit().putBoolean(PREF_USE_BLUETOOTH, useBluetooth).apply();
        final BluetoothController controller = ((JenkinsBotApplication) context.getApplicationContext()).getBluetoothController();
        if (useBluetooth) {
            controller.start();
        } else {
            controller.stop();
        }
    }

    public void setServerUrl(String serverUrl) {
        mServerUrl = serverUrl;

        prefs.edit().putString(PREF_SERVER_URL, serverUrl).apply();
    }

    boolean isUseBluetooth() {
        return useBluetooth;
    }

    public String getServerUrl() {
        return mServerUrl;
    }

    public String getUser() {
        return mUser;
    }

    public String getPassword() {
        return mPassword;
    }

    public boolean isInitialized() {
        return !TextUtils.isEmpty(mServerUrl) && !TextUtils.isEmpty(mUser) && !TextUtils.isEmpty(mPassword);
    }

    public void setUser(String user) {
        this.mUser = user;

        prefs.edit().putString(PREF_USER, user).apply();
    }

    public void setPassword(String password) {
        this.mPassword = password;

        prefs.edit().putString(PREF_PASSWORD, password).apply();
    }
}
