package com.vatsal.reactive.issue33157;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@SpringBootTest
public class MainTests {

    private final MockWebServer server = new MockWebServer();

    @BeforeEach
    void startServer() throws IOException {
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Set-Cookie", "Both=1; Expires=Tue, 22 Oct 2024 07:28:00 GMT; Max-Age=2592000")
                .addHeader("Set-Cookie", "Only_Expires=2; Expires=Tue, 22 Oct 2024 07:28:00 GMT")
                .addHeader("Set-Cookie", "Only_MaxAge=3; Max-Age=2592000")
                .setBody("{\"name\": \"Vatsal\"}");

        server.enqueue(response);
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    /*
    HttpComponentsClientHttpResponse.adaptCookies() ignores expires attribute from org.apache.hc.client5.http.cookie.Cookie
    Apache is not merging max age and expires, so we should keep both?
    ResponseCookie.toString(), expires is printed as max age. This seems misleading as max age is copied over to "expires" keyword.
     */
    @Test
    void testApacheHTTPComponents() {
        HttpComponentsClientHttpConnector httpComponentsClientHttpConnector = new HttpComponentsClientHttpConnector();

        URI uri = this.server.url("/").uri();
        Mono<ClientHttpResponse> futureResponse = httpComponentsClientHttpConnector.connect(HttpMethod.GET, uri, ReactiveHttpOutputMessage::setComplete);

        //futureResponse.block().getCookies().get("Both").get(0).getMaxAge();

        System.out.println(futureResponse.block().getCookies());
    }

    /*
    Netty ClientCookieDecoder.mergeMaxAgeAndExpires() merges maxAge and expires into maxAge.
    Therefore, ReactorClientHttpResponse.getCookies() only considers maxAge which is okay.
    Like previous test case, ResponseCookie.toString(), expires is printed as max age. This seems misleading as max age is copied over to "expires" keyword.
     */
    @Test
    void testReactorClient() {
        ReactorClientHttpConnector reactorClientHttpConnector = new ReactorClientHttpConnector();

        URI uri = this.server.url("/").uri();
        Mono<ClientHttpResponse> futureResponse = reactorClientHttpConnector.connect(HttpMethod.GET, uri, ReactiveHttpOutputMessage::setComplete);

        System.out.println(futureResponse.block().getCookies());
    }
}
