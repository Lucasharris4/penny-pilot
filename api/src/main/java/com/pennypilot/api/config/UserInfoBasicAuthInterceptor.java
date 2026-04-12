package com.pennypilot.api.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

// RFC 7230 §5.3 bans userinfo in the request-target, and the JDK HttpClient
// enforces that by silently dropping user:pass@ from every outbound URI.
// SimpleFIN's access URLs ship credentials as userinfo and assume the client
// will convert them to an Authorization header — which OkHttp, Apache
// HttpClient, and others do automatically. This interceptor restores that
// conversion for any RestClient built through our RestClientCustomizer chain.
//
// LOG HYGIENE: any URI that reaches this interceptor may contain the user's
// bank credentials in its userinfo component. Never log a URI from code that
// runs *before* this interceptor, and never include a caught RestClientException's
// message in logs — Spring's ResourceAccessException renders the failing URI
// verbatim, which would leak the password on a connection error.
public class UserInfoBasicAuthInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        URI uri = request.getURI();
        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo == null || rawUserInfo.isEmpty()) {
            return execution.execute(request, body);
        }

        HttpRequestWrapper rewrapped = stripUserInfo(request, uri);

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            String decoded = URLDecoder.decode(rawUserInfo, StandardCharsets.UTF_8);
            String authValue = "Basic " + Base64.getEncoder()
                    .encodeToString(decoded.getBytes(StandardCharsets.UTF_8));
            rewrapped.getHeaders().set(HttpHeaders.AUTHORIZATION, authValue);
        }

        return execution.execute(rewrapped, body);
    }

    private static HttpRequestWrapper stripUserInfo(HttpRequest request, URI uri) {
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() >= 0) {
            sb.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null) {
            sb.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            sb.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            sb.append('#').append(uri.getRawFragment());
        }
        URI clean = URI.create(sb.toString());

        return new HttpRequestWrapper(request) {
            @Override
            public URI getURI() {
                return clean;
            }
        };
    }
}
