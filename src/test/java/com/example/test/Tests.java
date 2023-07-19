package com.example.test;


import feign.RequestLine;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class Tests {

    private static final String MOCK_URL = "http://localhost:8080/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private TestService testService;

    @Before
    public void setUp() {
        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallbackFactory(e -> createLambdaFallback())
            .build();

        this.testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
    }

    @Test
    public void testFallback() {
        setupStub();

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    private void setupStub() {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "text/plain")
                .withBody("Hello, world!")));
    }

    public static TestService createLambdaFallback() {
        return () -> "fallback";
    }

    public interface TestService {

        @RequestLine("GET /greeting")
        String greeting();
    }

}
