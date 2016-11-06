package com.siju.acexplorer.filesystem.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.zip.ZipEntry;

public class ZipModel implements Parcelable {

    private final boolean directory;
    private final ZipEntry entry;
    private String name;
    private long date;
    private long size;

    public ZipModel(ZipEntry entry, long date, long size, boolean directory) {
        this.directory = directory;
        this.entry = entry;
        if (entry != null) {
            name = entry.getName();
            this.date = date;
            this.size = size;

        }
    }

    public ZipEntry getEntry() {
        return entry;
    }

    public boolean isDirectory() {
        return directory;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getTime() {
        return date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p1, int p2) {
        p1.writeString(name);
        p1.writeLong(size);
        p1.writeLong(date);
        p1.writeInt(isDirectory() ? 1 : 0);
    }

    public static final Parcelable.Creator<ZipModel> CREATOR =
            new Parcelable.Creator<ZipModel>() {
                public ZipModel createFromParcel(Parcel in) {
                    return new ZipModel(in);
                }

                public ZipModel[] newArray(int size) {
                    return new ZipModel[size];
                }
            };

    private ZipModel(Parcel im) {
        name = im.readString();
        size = im.readLong();
        date = im.readLong();
        int i = im.readInt();
        directory = i != 0;
        entry = new ZipEntry(name);
    }


}
