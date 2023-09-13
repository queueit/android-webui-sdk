package com.queue_it.androidsdk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(RobolectricTestRunner.class)
public class UriOverriderTest {

    @Test
    public void givenUserNavigatesToBackLink_ThenBackCallbackShouldBeCalled() {
        UriOverrider testObj = new UriOverrider();
        testObj.setQueue(Uri.parse("https://vavatest.queue-it.net/app/enqueue"));
        testObj.setTarget(Uri.parse("https://google.com"));
        WebView webView = mock(WebView.class);
        final AtomicBoolean queuePassed = new AtomicBoolean(false);
        final AtomicBoolean closeHandled = new AtomicBoolean(false);
        boolean loadCancelled = testObj.handleNavigationRequest("queueit://close", webView, new UriOverrideWrapper() {
            @Override
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {
                closeHandled.set(true);
            }

            @Override
            protected void onSessionRestart() {

            }
        });

        assertTrue(loadCancelled);
        assertFalse(queuePassed.get());
        assertTrue(closeHandled.get());
    }

    @Test
    public void givenUserNavigatesToSessionRestartLink_ThenSessionRestartCallbackShouldBeCalled() {
        UriOverrider testObj = new UriOverrider();
        testObj.setQueue(Uri.parse("https://vavatest.queue-it.net/app/enqueue"));
        testObj.setTarget(Uri.parse("https://google.com"));
        WebView webView = mock(WebView.class);
        final AtomicBoolean queuePassed = new AtomicBoolean(false);
        final AtomicBoolean sessionRestarted = new AtomicBoolean(false);
        boolean loadCancelled = testObj.handleNavigationRequest("queueit://restartSession", webView, new UriOverrideWrapper() {
            @Override
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {
                sessionRestarted.set(true);
            }
        });

        assertTrue(loadCancelled);
        assertFalse(queuePassed.get());
        assertTrue(sessionRestarted.get());
    }

    @Test
    public void givenUserIsNavigatingToBlockedPage_ThenLoadShouldBeCancelled() {
        String destinationUrl = "https://queue-it.com/what-is-this.html?customerId=vavatest&eventId=testendedroom&queueId=00000000-0000-0000-0000-000000000000&language=en-US";
        UriOverrider testObj = new UriOverrider();
        testObj.setQueue(Uri.parse("https://vavatest.queue-it.net/app/enqueue"));
        testObj.setTarget(Uri.parse("https://google.com"));
        WebView webView = mock(WebView.class);
        final AtomicBoolean queuePassed = new AtomicBoolean(false);
        boolean loadCancelled = testObj.handleNavigationRequest(destinationUrl, webView, new UriOverrideWrapper() {
            @Override
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

            }
        });

        assertTrue(loadCancelled);
        assertFalse(queuePassed.get());
    }

    @Test
    public void givenuserIsNavigatingToUrlOnTargetDomainButNotTargetUrl_ThenQueueSHouldNotBePassedAndWebBrowserShouldOpen() {
        String destinationUrl = "https://queue-it.com/what-is-this.html?customerId=vavatest&eventId=testendedroom&queueId=00000000-0000-0000-0000-000000000000&language=en-US";
        UriOverrider testObj = new UriOverrider();
        testObj.setQueue(Uri.parse("https://vavatest.queue-it.net/app/enqueue"));
        testObj.setTarget(Uri.parse("https://google.com/q=iamthetarget"));
        WebView webView = mock(WebView.class);
        final AtomicBoolean queuePassed = new AtomicBoolean(false);

        boolean loadCancelled = testObj.handleNavigationRequest(destinationUrl, webView, new UriOverrideWrapper() {
            @Override
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {
            }
        });

        assertTrue(loadCancelled);
        assertFalse(queuePassed.get());
    }

    @Test
    public void givenUserIsRedirectedToTargetLoadShouldBeCancelled() {
        UriOverrider testObj = new UriOverrider();
        testObj.setQueue(Uri.parse("https://useraccount.queue-it.net/app/enqueue"));
        testObj.setTarget(Uri.parse("https://google.com"));
        WebView webView = getMockedWebview();
        final AtomicBoolean queuePassed = new AtomicBoolean(false);
        boolean loadCancelled = testObj.handleNavigationRequest("https://google.com?queueittoken=a", webView, new UriOverrideWrapper() {
            @Override
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

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
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

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
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

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
        boolean loadCancelled = testObj.handleNavigationRequest("https://mypage.com?queueittoken=1", webView, new UriOverrideWrapper() {
            @Override
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

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
            protected void onQueueUrlChange(String uri) {
                System.out.print(uri);
            }

            @Override
            protected void onPassed(String queueItToken) {
                queuePassed.set(true);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

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
            protected void onQueueUrlChange(String uri) {
                urlChangeHappened.set(true);
            }

            @Override
            protected void onPassed(String queueItToken) {
                System.out.print(queueItToken);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

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
            protected void onQueueUrlChange(String uri) {
                urlChangeHappened.set(true);
            }

            @Override
            protected void onPassed(String queueItToken) {
                System.out.print(queueItToken);
            }

            @Override
            protected void onCloseClicked() {

            }

            @Override
            protected void onSessionRestart() {

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

        boolean loadCancelled = testObj.handleNavigationRequest("http://customer.queue-it.net/exitline.aspx?c=customer&e=otherroom2&q=qid&cid=en-US&l=myLayout&sdkv=Android-2.0.22",
                webView, new UriOverrideWrapper() {
                    @Override
                    protected void onQueueUrlChange(String uri) {
                        urlChangeHappened.set(true);
                    }

                    @Override
                    protected void onPassed(String queueItToken) {
                        System.out.print(queueItToken);
                    }

                    @Override
                    protected void onCloseClicked() {

                    }

                    @Override
                    protected void onSessionRestart() {

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
        final String expectedRewrittenUrl = "http://customer.queue-it.net/exitline.aspx?userId=myuser1&c=customer&e=otherroom2&q=qid&cid=en-US&l=myLayout&sdkv=Android-2.0.22";

        boolean loadCancelled = testObj.handleNavigationRequest("http://customer.queue-it.net/exitline.aspx?c=customer&e=otherroom2&q=qid&cid=en-US&l=myLayout&sdkv=Android-2.0.22",
                webView, new UriOverrideWrapper() {
                    @Override
                    protected void onQueueUrlChange(String uri) {
                        assertEquals(expectedRewrittenUrl, uri);
                    }

                    @Override
                    protected void onPassed(String queueItToken) {
                    }

                    @Override
                    protected void onCloseClicked() {

                    }

                    @Override
                    protected void onSessionRestart() {

                    }
                });

        verify(webView).loadUrl(argument.capture());
        assertEquals(expectedRewrittenUrl, argument.getValue());
    }
}