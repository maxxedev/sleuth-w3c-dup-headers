package com.example.demo;

import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class GreetingWebClient {

    private static final int SERVER_SLEEP_SECONDS = 5;

    public String getResultWithReadTimeOut(Duration duration) {
        return "client result = " + exchange(duration)
                .flatMap(res -> res.bodyToMono(String.class))
                .block();
    }

    public String getResultWithReactorTimeOut(Duration duration) {
        return "client result = " + exchange(duration)
                .flatMap(res -> res.bodyToMono(String.class))
                .timeout(duration)
                .block();
    }

    private Mono<ClientResponse> exchange(Duration readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(tcpClient -> {
                    tcpClient = tcpClient.doOnConnected(
                            conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS)));
                    return tcpClient;
                });

        // create a client http connector using above http client
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        // use this configured http connector to build the web client
        WebClient client = WebClient.builder()
                .clientConnector(connector)
                .build();

        return client.get()
                .uri("http://localhost:8080/hello?sleep=" + SERVER_SLEEP_SECONDS)
                .accept(MediaType.TEXT_PLAIN)
                .exchange();
    }
}
