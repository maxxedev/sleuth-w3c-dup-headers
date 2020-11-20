package com.example.demo;

import brave.baggage.BaggageField;
import brave.baggage.CorrelationScopeConfig.SingleCorrelationField;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext.ScopeDecorator;
import brave.sampler.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.api.http.HttpResponseParser;
import org.springframework.cloud.sleuth.instrument.web.HttpServerResponseParser;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class DemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    private static final AtomicBoolean foundDupHeaders = new AtomicBoolean();
    
    public static void main(String... args) throws Exception {

        ConfigurableApplicationContext context = new SpringApplicationBuilder(DemoApplication.class)
                .bannerMode(Mode.OFF)
                .run();

        logger.info("starting ...");

        // uninstrumented rest template
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        try {
            GreetingController.execHttpRequest(restTemplate, "http://localhost:8080/left");
        } finally {
            context.close();
        }
        
        logger.info("Found dup headers: {}", foundDupHeaders);
    }

    @Bean
    public RestTemplate restTemplate(HttpComponentsClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory();
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter() {

            @Override
            protected void beforeRequest(HttpServletRequest request, String message) {
                super.beforeRequest(request, message);
                
                String baggage = request.getHeader("baggage");
                String myHeader = request.getHeader("my-header");
                if (trimToEmpty(baggage).contains("my-header=new-value") && 
                        trimToEmpty(myHeader).contains("new-value")) {
                    logger.error("");
                    logger.error("");
                    logger.error("--- DUPLICATED HEADER VALUES FOUND ---");
                    logger.error("--- DUPLICATED HEADER VALUES FOUND ---");
                    logger.error("--- DUPLICATED HEADER VALUES FOUND ---");
                    logger.error("");
                    logger.error("");
                    foundDupHeaders.set(true);
                }
            }
            @Override
            protected void afterRequest(HttpServletRequest request, String message) {
            }
        };
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setIncludeHeaders(true);
        return loggingFilter;
    }

    private static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    @Bean(name = HttpServerResponseParser.NAME)
    public HttpResponseParser myHttpResponseParser() {
        return (response, context, span) -> {
            Object unwrap = response.unwrap();

            if (unwrap instanceof HttpServletResponse) {
                HttpServletResponse resp = (HttpServletResponse) unwrap;
                String headerValue = MDC.get("my-header");
                resp.addHeader("my-header", headerValue + ":" + System.currentTimeMillis());
            }
        };
    }

    @Bean
    public Sampler defaultSampler() {
        return Sampler.ALWAYS_SAMPLE;
    }

    @Bean
    public BaggageField baggageField() {
        return BaggageField.create("my-header");
    }

    @Bean
    ScopeDecorator scopeDecorator() {
        return MDCScopeDecorator.newBuilder()
                .clear()
                .add(SingleCorrelationField.newBuilder(baggageField())
                        .flushOnUpdate()
                        .build())
                .add(SingleCorrelationField.newBuilder(BaggageField.create("my-header2"))
                        .flushOnUpdate()
                        .build())
                .build();
    }

}
