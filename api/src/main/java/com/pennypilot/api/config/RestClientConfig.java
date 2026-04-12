package com.pennypilot.api.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;

@Configuration
public class RestClientConfig {

    // JDK HttpClient defaults to Redirect.NEVER, which means Spring's RestClient
    // silently drops 3xx responses on every call — a SimpleFIN URL move once
    // shipped garbage into encrypted credential storage before we noticed. NORMAL
    // refuses HTTPS->HTTP downgrades, which is the right posture for a financial
    // integration.
    @Bean
    public RestClientCustomizer restClientRedirectCustomizer() {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return builder -> builder.requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }

    @Bean
    public RestClientCustomizer restClientBasicAuthCustomizer() {
        return builder -> builder.requestInterceptor(new UserInfoBasicAuthInterceptor());
    }
}
