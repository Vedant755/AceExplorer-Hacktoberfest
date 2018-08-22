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

package com.siju.acexplorer.storage.model.backstack;

import com.siju.acexplorer.logging.Logger;
import com.siju.acexplorer.main.model.groups.Category;
import com.siju.acexplorer.storage.model.BackStackModel;

import java.util.ArrayList;


public class BackStackInfo {
    private final ArrayList<BackStackModel> backStack = new ArrayList<>();
    private static final String TAG = "BackStackInfo";

    public void addToBackStack(String path, Category category) {
        backStack.add(new BackStackModel(path, category));
        Logger.log(TAG, "addToBackStack--size=" + backStack.size() + " Path=" + path + "Category=" + category);
    }

    public void clearBackStack() {
         backStack.clear();
    }

    public  ArrayList<BackStackModel> getBackStack() {
        return backStack;
    }

    public void removeEntryAtIndex(int index) {
        if (index == -1 || index >= backStack.size()) {
            return;
        }
        Logger.log(TAG, "removeEntryAtIndex: "+ index + "backstackSize:" + backStack.size() + " path:"+backStack.get(index).getFilePath());
        backStack.remove(index);
    }

    public String getDirAtPosition(int index) {
        return backStack.get(index).getFilePath();
    }

    public Category getCategoryAtPosition(int index) {
        return backStack.get(index).getCategory();
    }



}
