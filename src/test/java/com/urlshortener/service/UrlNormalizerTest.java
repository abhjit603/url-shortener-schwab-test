package com.urlshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void normalizesSchemeHostPortFragmentAndWhitespace() {
        assertEquals(
                "https://example.com/path?q=1",
                normalizer.normalize("  HTTPS://Example.com:443/path?q=1#fragment  "));
    }

    @Test
    void addsSlashAndConvertsInternationalHostToAscii() {
        assertEquals("http://xn--bcher-kva.example/", normalizer.normalize("http://bücher.example"));
    }

    @Test
    void preservesEscapedPathAndQueryCharacters() {
        assertEquals(
                "https://example.com/a%2Fb?q=x%26y",
                normalizer.normalize("HTTPS://Example.com/a%2Fb?q=x%26y#fragment"));
    }

    @Test
    void rejectsMissingMalformedAndUnsupportedDestinations() {
        assertInvalid(null);
        assertInvalid("   ");
        assertInvalid("relative/path");
        assertInvalid("ftp://example.com/file");
        assertInvalid("https://user:secret@example.com/");
        assertInvalid("https://@example.com/");
        assertInvalid("https:///path");
        assertInvalid("https://example.com/" + "a".repeat(2048));
    }

    private void assertInvalid(String destination) {
        ApiException exception = assertThrows(ApiException.class, () -> normalizer.normalize(destination));
        assertEquals(ErrorCode.INVALID_URL, exception.code());
        assertEquals("destination", exception.validationErrors().get(0).field());
    }
}
