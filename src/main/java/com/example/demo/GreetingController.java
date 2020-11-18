package com.example.demo;

import brave.baggage.BaggageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GreetingController {

    private static final Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BaggageField baggageField;

    @GetMapping("/left")
    public String left() throws Exception {
        logger.info("processinging left");

        logger.info("mdc {}", MDC.getCopyOfContextMap());

        execHttpRequest(restTemplate, "http://localhost:8080/middle");
        logger.info("processed left");
        return "left";
    }

    @GetMapping("/middle")
    public String middle() throws Exception {

        logger.info("processinging middle");

        baggageField.updateValue("new-value");
        logger.info("mdc {}", MDC.getCopyOfContextMap());

        execHttpRequest(restTemplate, "http://localhost:8080/right");
        logger.info("processed middle");
        return "middle";
    }

    @GetMapping("/right")
    public String right() {
        logger.info("processinging right");
        logger.info("mdc {}", MDC.getCopyOfContextMap());

//        execHttpRequest(restTemplate, "https://httpbin.org/headers");
        logger.info("processed right");
        return "right";
    }

    public static void execHttpRequest(RestTemplate restTemplate, String url) {
        execHttpRequest(restTemplate, url, new HttpHeaders());
    }

    public static void execHttpRequest(RestTemplate restTemplate, String url, MultiValueMap<String, String> requestHeaders) {
        logger.info("Sending request to {}", url);
        
        ResponseEntity<String> entity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders), String.class);

        HttpHeaders headers = entity.getHeaders();

        headers.forEach((key, value) -> logger.info("{}: {}", key, value));
        logger.info("");
        logger.info("{}", entity.getBody());
        logger.info("");
    }
}
