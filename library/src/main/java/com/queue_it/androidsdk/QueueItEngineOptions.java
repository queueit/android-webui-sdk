package com.queue_it.androidsdk;

import android.os.Parcel;
import android.os.Parcelable;

public class QueueItEngineOptions implements Parcelable {
    private boolean disableBackButtonFromWR;
    private String sdkUserAgent;

    public QueueItEngineOptions() {
    }

    public QueueItEngineOptions(boolean disableBackButtonFromWR) {
        this.disableBackButtonFromWR = disableBackButtonFromWR;
        this.sdkUserAgent = "";
    }

    public QueueItEngineOptions(String sdkUserAgent) {
        this.disableBackButtonFromWR = true;
        this.sdkUserAgent = sdkUserAgent;
    }

    public QueueItEngineOptions(boolean disableBackButtonFromWR, String sdkUserAgent) {
        this.disableBackButtonFromWR = disableBackButtonFromWR;
        this.sdkUserAgent = sdkUserAgent;
    }

    protected QueueItEngineOptions(Parcel in) {
        disableBackButtonFromWR = in.readInt() != 0;
        sdkUserAgent = in.readString();
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

    /**
     * @deprecated Use {@link #getSdkUserAgent()} instead. This method will be removed in a future version.
     */
    @Deprecated
    public String getWebViewUserAgent() {
        return sdkUserAgent;
    }

    /**
     * @deprecated Use {@link #setSdkUserAgent(String sdkUserAgent)} instead. This method will be removed in a future version.
     */
    @Deprecated
    public void setWebViewUserAgent(String sdkUserAgent) {
        this.sdkUserAgent = sdkUserAgent;
    }

    public String getSdkUserAgent() {
        return sdkUserAgent;
    }

    public void setSdkUserAgent(String sdkUserAgent) {
        this.sdkUserAgent = sdkUserAgent;
    }

    public static QueueItEngineOptions getDefault() {
        return new QueueItEngineOptions(true, "");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.disableBackButtonFromWR ? 1 : 0);
        dest.writeString(this.sdkUserAgent);
    }
}
