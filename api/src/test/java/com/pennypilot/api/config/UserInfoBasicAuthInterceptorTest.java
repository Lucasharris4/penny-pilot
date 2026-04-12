package com.pennypilot.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserInfoBasicAuthInterceptorTest {

    private final UserInfoBasicAuthInterceptor interceptor = new UserInfoBasicAuthInterceptor();

    @Test
    void userInfoPresent_setsBasicAuthHeaderAndStripsUserInfoFromUri() throws IOException {
        HttpRequest request = stubRequest(URI.create("https://user:pass@bridge.simplefin.org/simplefin/accounts"));
        RecordingExecution execution = new RecordingExecution();

        interceptor.intercept(request, new byte[0], execution);

        HttpRequest sent = execution.captured.get();
        assertEquals(URI.create("https://bridge.simplefin.org/simplefin/accounts"), sent.getURI());
        // base64("user:pass") = dXNlcjpwYXNz
        assertEquals("Basic dXNlcjpwYXNz", sent.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void userInfoAbsent_passesThroughUnchanged() throws IOException {
        URI original = URI.create("https://bridge.simplefin.org/simplefin/accounts");
        HttpRequest request = stubRequest(original);
        RecordingExecution execution = new RecordingExecution();

        interceptor.intercept(request, new byte[0], execution);

        HttpRequest sent = execution.captured.get();
        assertEquals(original, sent.getURI());
        assertNull(sent.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void preExistingAuthorizationHeader_isNotClobbered() throws IOException {
        HttpRequest request = stubRequest(URI.create("https://user:pass@bridge.simplefin.org/simplefin/accounts"));
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer external-token");
        RecordingExecution execution = new RecordingExecution();

        interceptor.intercept(request, new byte[0], execution);

        HttpRequest sent = execution.captured.get();
        // URI is still stripped so the header doesn't travel in the request-target...
        assertEquals(URI.create("https://bridge.simplefin.org/simplefin/accounts"), sent.getURI());
        // ...but the caller-supplied Authorization wins.
        assertEquals("Bearer external-token", sent.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void urlEncodedUserInfo_isDecodedBeforeBase64() throws IOException {
        // Password contains URL-encoded '@' (%40) and '+' (%2B)
        HttpRequest request = stubRequest(URI.create("https://alice:p%40ss%2Bword@bridge.simplefin.org/x"));
        RecordingExecution execution = new RecordingExecution();

        interceptor.intercept(request, new byte[0], execution);

        HttpRequest sent = execution.captured.get();
        // base64("alice:p@ss+word") = YWxpY2U6cEBzcyt3b3Jk
        assertEquals("Basic YWxpY2U6cEBzcyt3b3Jk", sent.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void portAndQueryArePreservedWhenStrippingUserInfo() throws IOException {
        HttpRequest request = stubRequest(URI.create("https://user:pass@bridge.simplefin.org:8443/x/y?a=1&b=2"));
        RecordingExecution execution = new RecordingExecution();

        interceptor.intercept(request, new byte[0], execution);

        HttpRequest sent = execution.captured.get();
        assertEquals(URI.create("https://bridge.simplefin.org:8443/x/y?a=1&b=2"), sent.getURI());
        assertFalse(sent.getURI().toString().contains("user:pass"));
    }

    private static HttpRequest stubRequest(URI uri) {
        return new HttpRequest() {
            private final HttpHeaders headers = new HttpHeaders();

            @Override
            public HttpMethod getMethod() {
                return HttpMethod.GET;
            }

            @Override
            public URI getURI() {
                return uri;
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }

            @Override
            public java.util.Map<String, Object> getAttributes() {
                return new java.util.HashMap<>();
            }
        };
    }

    private static class RecordingExecution implements ClientHttpRequestExecution {
        final AtomicReference<HttpRequest> captured = new AtomicReference<>();

        @Override
        public ClientHttpResponse execute(HttpRequest request, byte[] body) {
            captured.set(request);
            return null;
        }
    }
}
