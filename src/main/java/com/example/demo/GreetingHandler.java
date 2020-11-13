package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class GreetingHandler {

    private static final Logger logger = LoggerFactory.getLogger(GreetingHandler.class);

    public Mono<ServerResponse> hello(ServerRequest request) {
        var duration = request.queryParam("sleep")
                .map(Integer::parseInt)
                .orElse(0);

        try {
            logger.info("Server sleeping for {}s", duration);
            Thread.sleep(duration * 1000);
        } catch (InterruptedException e) {
            logger.error("interrupted", e);
        }

        logger.info("Server responding...");
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromValue("Hello, Spring!"));
    }
}
