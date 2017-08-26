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

package com.siju.acexplorer.filesystem.groups;

public enum Category {
    FILES(0),
    AUDIO(1),
    VIDEO(2),
    IMAGE(3),
    DOCS(4),
    DOWNLOADS(5),
    ADD(6),
    COMPRESSED(7),
    FAVORITES(8),
    PDF(9),
    APPS(10),
    LARGE_FILES(11),
    ZIP_VIEWER(12),
    GENERIC_LIST(13),
    PICKER(14);

    private final int value;

    Category(int value) {
        this.value = value;
    }

    public int getValue() {

        return value;
    }

    public static Category getCategory(int position) {
        switch (position) {
            case 0:
                return FILES;
            case 1:
                return AUDIO;
            case 2:
                return VIDEO;
            case 3:
                return IMAGE;
            case 4:
                return DOCS;
            case 5:
                return DOWNLOADS;
            case 7:
                return COMPRESSED;
            case 8:
                return FAVORITES;
            case 9:
                return PDF;
            case 10:
                return APPS;
            case 11:
                return LARGE_FILES;
            case 12:
                return ZIP_VIEWER;
            case 13:
                return GENERIC_LIST;
            case 14:
                return PICKER;
        }
        return FILES;
    }

    public static boolean checkIfFileCategory(Category category) {
        return category.equals(FILES) ||
                category.equals(COMPRESSED) ||
                category.equals(DOWNLOADS) ||
                category.equals(FAVORITES) ||
                category.equals(LARGE_FILES);
    }
}
