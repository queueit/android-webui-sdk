package com.queue_it.androidsdk;

import android.os.Parcel;
import android.os.Parcelable;

public class QueueItEngineOptions implements Parcelable {
    private boolean disableBackButtonFromWR;
    private String webViewUserAgent;

    public QueueItEngineOptions() {
    }

    public QueueItEngineOptions(boolean disableBackButtonFromWR) {
        this.disableBackButtonFromWR = disableBackButtonFromWR;
        this.webViewUserAgent = "";
    }
    public QueueItEngineOptions(String webViewUserAgent) {
        this.disableBackButtonFromWR = true;
        this.webViewUserAgent = webViewUserAgent;
    }
    public QueueItEngineOptions(boolean disableBackButtonFromWR, String webViewUserAgent) {
        this.disableBackButtonFromWR = disableBackButtonFromWR;
        this.webViewUserAgent = webViewUserAgent;
    }

    protected QueueItEngineOptions(Parcel in) {
        disableBackButtonFromWR = in.readInt() != 0;
        webViewUserAgent = in.readString();
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

    public String getWebViewUserAgent() {
        return webViewUserAgent;
    }

    public void setWebViewUserAgent(String webViewUserAgent) {
        this.webViewUserAgent = webViewUserAgent;
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
        dest.writeString(this.webViewUserAgent);
    }
}
