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
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;

@SpringBootApplication
public class DemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String... args) throws Exception {

        var context = new SpringApplicationBuilder(DemoApplication.class)
                .bannerMode(Mode.OFF)
                .properties("spring.sleuth.enabled=false")
                .run();

        logger.info("starting ...");
        var httpHeaders = new HttpHeaders();

        try {
            GreetingController.runHttpRequest(new RestTemplate(), "http://localhost:8080/middle", httpHeaders);
        } finally {
            context.close();
        }
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

    @Bean
    ResponseBodyAdvice<Object> upstreamPropagatingRequestFilterAdvice() {
        return new MyAdvice();
    }

    static BaggageField SOME_HEADER = BaggageField.create("some-header");

    @Bean
    ScopeDecorator scopeDecorator() {
        return MDCScopeDecorator.newBuilder()
                .clear()
                .add(SingleCorrelationField.newBuilder(SOME_HEADER)
                        .flushOnUpdate()
                        .build())
                .build();
    }

    @ControllerAdvice
    public static class MyAdvice implements ResponseBodyAdvice<Object> {

        @Override
        public boolean supports(final MethodParameter returnType, final Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object beforeBodyWrite(final Object body,
                final MethodParameter returnType,
                final MediaType selectedContentType,
                final Class<? extends HttpMessageConverter<?>> selectedConverterType,
                final ServerHttpRequest request,
                final ServerHttpResponse response) {

            var headerValue = MDC.get("my-corr-id");
            response.getHeaders().set("my-corr-id", "zz " + headerValue + ":" + System.currentTimeMillis());

            return body;
        }
    }
}
