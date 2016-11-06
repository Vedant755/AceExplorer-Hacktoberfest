package com.siju.acexplorer.filesystem.model;

import android.os.Parcel;
import android.os.Parcelable;

public class BaseFile implements Parcelable {
    private final long date;
    private final long size;
    private final boolean isDirectory;
    private final String permission;
    private String name;
    private String path;

    private String link = "";

    public BaseFile(String path, String permission, long date, long size) {
        this.date = date;
        this.size = size;
        this.isDirectory = true;
        this.path = path;
        this.permission = permission;

    }


    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public long getDate() {
        return date;
    }

    public long getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }


    public String getPermission() {
        return permission;
    }

    private BaseFile(Parcel in) {
        permission = in.readString();
        name = in.readString();
        date = in.readLong();
        size = in.readLong();
        isDirectory = in.readByte() != 0;

    }

    public static final Creator<BaseFile> CREATOR = new Creator<BaseFile>() {
        @Override
        public BaseFile createFromParcel(Parcel in) {
            return new BaseFile(in);
        }

        @Override
        public BaseFile[] newArray(int size) {
            return new BaseFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(permission);
        dest.writeString(name);
        dest.writeLong(date);
        dest.writeLong(size);
        dest.writeByte((byte) (isDirectory ? 1 : 0));

    }
}
