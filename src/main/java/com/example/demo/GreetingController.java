package com.example.demo;

import brave.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.instrument.async.TraceRunnable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@RestController
public class GreetingController {

    private static final Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Tracing tracing;

    @GetMapping("/left")
    public String left() throws Exception {
        logger.info("processinging hello");

        logger.info("mdc {}", MDC.getCopyOfContextMap());

        runHttpRequest(restTemplate, "http://localhost:8080/middle");
        logger.info("processed hello");

        logger.info("replying hello");

        return "hello";
    }

    private static Runnable tracable(String name, Runnable delegate) {
        return new TraceRunnable(Tracing.current(), null, delegate, name);
    }

    @GetMapping("/middle")
    public String middle() throws Exception {

        logger.info("processinging middle");

        DemoApplication.SOME_HEADER.updateValue("new-value");
        logger.info("mdc {}", MDC.getCopyOfContextMap());

        CompletableFuture.runAsync(tracable("right", () -> runHttpRequest(restTemplate, "http://localhost:8080/right"))).get();
        logger.info("processed middle");
        return "hello";
    }

    @GetMapping("/right")
    public String right() {
        logger.info("processinging right");
        logger.info("mdc {}", MDC.getCopyOfContextMap());

        runHttpRequest(restTemplate, "https://httpbin.org/headers");
        logger.info("processed right");
        return "right";
    }

    public static void runHttpRequest(RestTemplate restTemplate, String url) {
        runHttpRequest(restTemplate, url, new HttpHeaders());
    }

    public static void runHttpRequest(RestTemplate restTemplate, String url, MultiValueMap<String, String> requestHeaders) {
        var entity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders), String.class);

        logger.info("");
        var headers = entity.getHeaders();

        headers.forEach((key, value) -> logger.info("{}: {}", key, value));
        logger.info("");
        logger.info("{}", entity.getBody());
        logger.info("");
    }
}
