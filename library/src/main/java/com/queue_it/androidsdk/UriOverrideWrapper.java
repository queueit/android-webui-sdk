package com.queue_it.androidsdk;

public abstract class UriOverrideWrapper{
  abstract void onQueueUrlChange(String uri);
  abstract void onPassed(String queueItToken);
}
