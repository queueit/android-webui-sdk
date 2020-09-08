package com.queue_it.androidsdk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UriOverriderTest {

  @Test
  public void givenUserIsRedirectedToTargetLoadShouldBeCancelled() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("https://useraccount.queue-it.net/app/enqueue"));
    testObj.setTarget(Uri.parse("https://google.com"));
    WebView webView = mock(WebView.class);
    final AtomicBoolean queuePassed = new AtomicBoolean(false);
    boolean loadCancelled = testObj.handleNavigationRequest("https://google.com", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        System.out.print(uri);
      }

      @Override
      void onPassed(String queueItToken) {
        queuePassed.set(true);
      }
    });

    assertTrue(loadCancelled);
    assertTrue(queuePassed.get());
  }

  @Test
  public void givenUserIsNavigatingToExternalPageThenLoadShouldBeCancelledAndIntentShouldBeStarted() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("https://useraccount.queue-it.net/app/enqueue"));
    testObj.setTarget(Uri.parse("https://google.com"));
    WebView webView = getMockedWebview();
    final AtomicBoolean queuePassed = new AtomicBoolean(false);
    ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
    String otherPage = "https://bing.com";

    boolean loadCancelled = testObj.handleNavigationRequest(otherPage, webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        System.out.print(uri);
      }

      @Override
      void onPassed(String queueItToken) {
        queuePassed.set(true);
      }
    });

    assertTrue(loadCancelled);
    assertFalse(queuePassed.get());

    verify(webView.getContext()).startActivity(argument.capture());
  }

  @Test
  public void givenAppUserIsRedirectedToDeepLinkThenLoadShouldBeCancelled() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("qapp://enqueue"));
    testObj.setTarget(Uri.parse("myapp://page1"));
    WebView webView = getMockedWebview();
    final AtomicBoolean queuePassed = new AtomicBoolean(false);

    boolean loadCancelled = testObj.handleNavigationRequest("myapp://page1", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        System.out.print(uri);
      }

      @Override
      void onPassed(String queueItToken) {
        queuePassed.set(true);
      }
    });

    assertTrue(loadCancelled);
    assertFalse(queuePassed.get());
  }

  @Test
  public void givenAppUserIsRedirectedToTargetThenLoadShouldBeCancelled() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("qapp://enqueue"));
    testObj.setTarget(Uri.parse("https://mypage.com"));
    WebView webView = mock(WebView.class);
    final AtomicBoolean queuePassed = new AtomicBoolean(false);
    boolean loadCancelled = testObj.handleNavigationRequest("https://mypage.com", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        System.out.print(uri);
      }

      @Override
      void onPassed(String queueItToken) {
        queuePassed.set(true);
      }
    });

    assertTrue(loadCancelled);
    assertTrue(queuePassed.get());
  }

  private WebView getMockedWebview() {
    WebView webView = mock(WebView.class);
    final Context mockedContext = mock(Context.class);
    when(webView.getContext()).thenAnswer(new Answer<Context>() {
      @Override
      public Context answer(InvocationOnMock invocation) {
        return mockedContext;
      }
    });
    return webView;
  }

  @Test
  public void givenUserIsRedirectedToDeepLinkThenLoadShouldBeCancelled() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("https://useraccount.queue-it.net/app/enqueue"));
    testObj.setTarget(Uri.parse("myapp://page1"));
    WebView webView = getMockedWebview();

    final AtomicBoolean queuePassed = new AtomicBoolean(false);
    boolean loadCancelled = testObj.handleNavigationRequest("myapp://page1", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        System.out.print(uri);
      }

      @Override
      void onPassed(String queueItToken) {
        queuePassed.set(true);
      }
    });

    assertTrue(loadCancelled);
    assertFalse(queuePassed.get());
  }

  @Test
  public void givenUserIsNavigatingToExternalDeepUrlThenLoadShouldBeCancelledAndIntentShouldBeStarted() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("https://useraccount.queue-it.net/app/enqueue"));
    testObj.setTarget(Uri.parse("https://google.com"));
    testObj.setUserId("myuser1");
    WebView webView = getMockedWebview();
    ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);

    final AtomicBoolean urlChangeHappened = new AtomicBoolean(false);
    boolean loadCancelled = testObj.handleNavigationRequest("myapp://go?tab=activity1", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        urlChangeHappened.set(true);
      }

      @Override
      void onPassed(String queueItToken) {
        System.out.print(queueItToken);
      }
    });

    assertTrue(loadCancelled);
    assertFalse(urlChangeHappened.get());

    verify(webView.getContext()).startActivity(argument.capture());
  }

  @Test
  public void givenUserIsRedirectedToLeaveLineThenLoadShouldBeCancelled() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("https://useraccount.queue-it.net/app/enqueue"));
    testObj.setTarget(Uri.parse("https://google.com"));
    testObj.setUserId("myuser1");
    WebView webView = mock(WebView.class);

    final AtomicBoolean urlChangeHappened = new AtomicBoolean(false);
    boolean loadCancelled = testObj.handleNavigationRequest("https://useraccount.queue-it.net/app/leaveLine", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        urlChangeHappened.set(true);
      }

      @Override
      void onPassed(String queueItToken) {
        System.out.print(queueItToken);
      }
    });

    assertTrue(loadCancelled);
    assertTrue(urlChangeHappened.get());
  }

  @Test
  public void givenUserNavigatesToExitLineThenLoadShouldCancel_AndQueueShouldNotBePassed() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("https://customer.queue-it.net/app/enqueue"));
    testObj.setTarget(Uri.parse("http://localhost:3000"));
    testObj.setUserId("myuser1");
    WebView webView = mock(WebView.class);

    final AtomicBoolean urlChangeHappened = new AtomicBoolean(false);
    final AtomicBoolean queuePassed = new AtomicBoolean(false);

    boolean loadCancelled = testObj.handleNavigationRequest("http://customer.queue-it.net/exitline.aspx?c=customer&e=otherroom2&q=qid&cid=en-US&l=myLayout&sdkv=Android-2.0.22", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        urlChangeHappened.set(true);
      }

      @Override
      void onPassed(String queueItToken) {
        System.out.print(queueItToken);
      }
    });
    assertTrue(loadCancelled);
    assertTrue(urlChangeHappened.get());
    assertFalse(queuePassed.get());
  }


  @Test
  public void givenUserIsNavigatingToQueueItPageUrlShouldIncludeUserId() {
    UriOverrider testObj = new UriOverrider();
    testObj.setQueue(Uri.parse("https://customer.queue-it.net/app/enqueue"));
    testObj.setTarget(Uri.parse("http://localhost:3000"));
    testObj.setUserId("myuser1");
    WebView webView = mock(WebView.class);

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    final String expectedRewrittenUrl = "http://customer.queue-it.net/exitline.aspx?c=customer&e=otherroom2&q=qid&cid=en-US&l=myLayout&sdkv=Android-2.0.22&userId=myuser1";

    boolean loadCancelled = testObj.handleNavigationRequest("http://customer.queue-it.net/exitline.aspx?c=customer&e=otherroom2&q=qid&cid=en-US&l=myLayout&sdkv=Android-2.0.22", webView, new UriOverrideWrapper() {
      @Override
      void onQueueUrlChange(String uri) {
        assertEquals(expectedRewrittenUrl, uri);
      }

      @Override
      void onPassed(String queueItToken) {
      }
    });

    verify(webView).loadUrl(argument.capture());
    assertEquals(expectedRewrittenUrl, argument.getValue());
  }

}