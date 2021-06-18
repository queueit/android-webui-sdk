package com.queue_it.androidsdk;

import android.os.Parcel;
import android.os.Parcelable;

public class QueueItEngineOptions implements Parcelable {
    private boolean disableBackButtonFromWR;

    public QueueItEngineOptions() {
    }

    public QueueItEngineOptions(boolean disableBackButtonFromWR) {
        this.disableBackButtonFromWR = disableBackButtonFromWR;
    }

    protected QueueItEngineOptions(Parcel in) {
        disableBackButtonFromWR = in.readInt() != 0;
    }

    public static final Creator<QueueItEngineOptions> CREATOR = new Creator<QueueItEngineOptions>() {
        @Override
        public QueueItEngineOptions createFromParcel(Parcel in) {
            return new QueueItEngineOptions(in);
        }

        @Override
        public QueueItEngineOptions[] newArray(int size) {
            return new QueueItEngineOptions[size];
        }
    };

    public boolean isBackButtonDisabledFromWR() {
        return disableBackButtonFromWR;
    }

    public void setBackButtonDisabledFromWR(boolean disableBackButtonFromWR) {
        this.disableBackButtonFromWR = disableBackButtonFromWR;
    }

    public static QueueItEngineOptions getDefault() {
        return new QueueItEngineOptions(true);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.disableBackButtonFromWR ? 1 : 0);
    }
}
