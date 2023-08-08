package com.mycompany.gainz.Database.Model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Exercise implements Comparable<Exercise>, Parcelable {
    private int _id;
    private String name;
    private String bodypart = "";
    private int defaultIncrement = 1;

    public Exercise(String name) {
        this.name = name;
    }

    public Exercise(String name, String bodypart) {
        this.name = name;
        this.bodypart = bodypart;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBodypart(String bodypart) {
        this.bodypart = bodypart;
    }

    public void setDefaultIncrement(int defaultIncrement) {
        this.defaultIncrement = defaultIncrement;
    }

    public int get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public String getBodypart() {
        return bodypart;
    }

    public int getDefaultIncrement() {
        return defaultIncrement;
    }

    @Override
    public int compareTo(@NonNull Exercise another) {
        return this.name.compareTo(another.name);
    }

    private Exercise(Parcel in) {
        int[] intData = new int[2];
        in.readIntArray(intData);
        this._id = intData[0];
        this.defaultIncrement = intData[1];

        String[] stringData = new String[2];
        in.readStringArray(stringData);
        this.name = stringData[0];
        this.bodypart = stringData[1];
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(new int[]{
                this._id,
                this.defaultIncrement
        });
        dest.writeStringArray(new String[]{
                this.name,
                this.bodypart
        });
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Exercise createFromParcel(Parcel in) {
            return new Exercise(in);
        }
        public Exercise[] newArray(int size) {
            return new Exercise[size];
        }
    };
}
