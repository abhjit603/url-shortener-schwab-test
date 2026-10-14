package com.urlshortener.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private static final String ALPHABET =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final int LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    public String nextCode() {
        char[] buf = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            buf[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(buf);
    }
}
