package com.urlshortener.web;

import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import com.urlshortener.service.StoreProbe;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final StoreProbe storeProbe;

    public HealthController(StoreProbe storeProbe) {
        this.storeProbe = storeProbe;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        try {
            storeProbe.assertReachable();
            return Map.of("status", "UP");
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.STORE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, "Data store is unavailable");
        }
    }
}
