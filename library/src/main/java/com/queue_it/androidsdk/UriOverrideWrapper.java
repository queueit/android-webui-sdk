package com.queue_it.androidsdk;

public abstract class UriOverrideWrapper{
  protected abstract void onQueueUrlChange(String uri);
  protected abstract void onPassed(String queueItToken);
  protected abstract void onCloseClicked();
  protected abstract void onSessionRestart();
}
