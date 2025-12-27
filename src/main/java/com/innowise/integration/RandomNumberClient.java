package com.innowise.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${external.random.url}")
    private String randomUrl;

    public int getRandomNumber() {
        Integer[] resp = restClientBuilder.build()
                .get()
                .uri(randomUrl)
                .retrieve()
                .body(Integer[].class);

        if (resp == null || resp.length == 0 || resp[0] == null) {
            log.info("Random API returned empty response");
            throw new IllegalStateException("Random API returned empty response");
        }
        log.info("Random API returned {}", resp[0]);
        return resp[0];
    }
}
