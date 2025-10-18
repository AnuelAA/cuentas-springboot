package com.cuentas.backend.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RequestSizeLoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RequestSizeLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        int requestBytes = body == null ? 0 : body.length;
        int requestChars = body == null ? 0 : new String(body, StandardCharsets.UTF_8).length();
        log.info("HTTP request {} {} size={} bytes ({} chars)", request.getMethod(), request.getURI(), requestBytes, requestChars);

        ClientHttpResponse response = execution.execute(request, body);

        // response body is bufferable if RestTemplate uses BufferingClientHttpRequestFactory
        byte[] respBody = StreamUtils.copyToByteArray(response.getBody());
        int respBytes = respBody == null ? 0 : respBody.length;
        log.info("HTTP response {} size={} bytes", response.getStatusCode(), respBytes);

        // recreate response with same body is not necessary when using buffering factory
        return response;
    }
}