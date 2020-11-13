package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Duration;
import java.util.concurrent.Callable;

@SpringBootApplication
public class DemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);

        var client = new GreetingWebClient();
        var timeout = Duration.ofSeconds(2);

        run(() -> client.getResultWithReactorTimeOut(timeout));
        run(() -> client.getResultWithReadTimeOut(timeout));
    }

    private static void run(Callable<String> callable) {

        try {
            logger.info(callable.call());
        } catch (Exception e) {
            logger.warn("", e);
        }
    }
}
