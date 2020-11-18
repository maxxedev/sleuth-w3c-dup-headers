package com.example.demo;

import brave.baggage.BaggageField;
import brave.baggage.CorrelationScopeConfig.SingleCorrelationField;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext.ScopeDecorator;
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

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class DemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    private static final AtomicInteger parserCallCount = new AtomicInteger();
    
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
        
        logger.info("parserCallCount = {}", parserCallCount);
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
            protected void afterRequest(HttpServletRequest request, String message) {
            }
        };
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setIncludeHeaders(true);
        return loggingFilter;
    }

    @Bean(name = HttpServerResponseParser.NAME)
    public HttpResponseParser myHttpResponseParser() {
        return (response, context, span) -> {
            logger.info("parsing http response");
            parserCallCount.incrementAndGet();
            Object unwrap = response.unwrap();
            if (unwrap instanceof HttpServletResponse) {
                HttpServletResponse resp = (HttpServletResponse) unwrap;
                String headerValue = MDC.get("my-header");
                resp.addHeader("my-header", headerValue + ":" + System.currentTimeMillis());
            }
        };
    }

    @Bean
    public BaggageField countryCodeField() {
        return BaggageField.create("my-header");
    }

    @Bean
    ScopeDecorator scopeDecorator() {
        return MDCScopeDecorator.newBuilder()
                .clear()
                .add(SingleCorrelationField.newBuilder(countryCodeField())
                        .flushOnUpdate()
                        .build())
                .build();
    }

}
