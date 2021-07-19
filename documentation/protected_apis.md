# Using connector protected APIs with the SDK

## Sample app

A sample app that shows an example of this integration can be found in the [demowithprotectedapi](https://github.com/queueit/android-webui-sdk/tree/master/demowithprotectedapi) directory.
There are a few OkHTTP interceptors in the `http` package that can be easily integrated.

## Implementation

To integrate with a protected API we need to handle the validation responses that we may get in case the user should be queued.  
All calls to protected APIs need to include the `x-queueit-ajaxpageurl` header with a non-empty value and a Queue-it accepted cookie (if present).
The integration can be described in the following steps:

1. API Request with is made
2. We get a response which may be the API response or a notice that the user should be queued 
3. Scenario 1, user should be queued  
3.1. If the user should be queued we'll get a 302 response with a `x-queueit-redirect` header. We need to extract the `c`(Customer ID) and `e`(Waiting Room ID) query string parameters from the header and call `QueueITEngine.run` with them, just as you would normally do with the SDK.  
3.2. We wait for the `onQueuePassed` callback and we store the QueueITToken.  
3.3. We can repeat the API request appending the `queueittoken={QueueITToken}` query string parameter.  
3.4. We store the Queue-it cookies from the response, so they can be set in other API calls.

4. Scenario 2, user should not be queued  
4.1. We store the Queue-it cookies from the response

![API Integration Flow](https://github.com/queueit/android-webui-sdk/blob/master/documentation/App%20+%20Connector%20integration%20with%20QueueITToken.png "App Integration Flow")
