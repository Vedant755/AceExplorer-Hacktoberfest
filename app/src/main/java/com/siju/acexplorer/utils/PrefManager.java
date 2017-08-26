/*
 * Copyright (C) 2017 Ace Explorer owned by Siju Sakaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.siju.acexplorer.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class PrefManager {
    private final SharedPreferences pref;

    // Shared preferences file name
    private static final String PREF_NAME = "ace_prefs";

    private static final String IS_FIRST_TIME_LAUNCH = "first_launch";

    public PrefManager(Context context) {
        int PRIVATE_MODE = 0;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
    }

    public void setFirstTimeLaunch() {
        pref.edit().putBoolean(IS_FIRST_TIME_LAUNCH, false).apply();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

}
