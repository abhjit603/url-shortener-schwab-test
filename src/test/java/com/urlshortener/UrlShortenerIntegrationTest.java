package com.urlshortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.repository.ClickBucketRepository;
import com.urlshortener.repository.ShortLinkRepository;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.LinkService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "app.base-url=http://short.test")
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    private static final String TEST_DATA_PATH = Path.of(
                    System.getProperty("java.io.tmpdir"), "url-shortener-it-" + UUID.randomUUID())
            .toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Autowired
    private ClickBucketRepository clickBucketRepository;

    @Autowired
    private LinkService linkService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("app.data-path", () -> TEST_DATA_PATH);
    }

    @BeforeEach
    void clearDatabase() {
        clickBucketRepository.deleteAll();
        shortLinkRepository.deleteAll();
    }

    @Test
    void healthAndProblemDetailsAreAvailable() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/admin/links'].get.parameters[?(@.name=='cursor')].name")
                .exists())
            .andExpect(jsonPath("$.paths['/{code}'].get").exists());

        mockMvc.perform(post("/api/admin/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destination\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INVALID_URL"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("destination"));

        mockMvc.perform(get("/api/admin/links/ZZZZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINK_NOT_FOUND"));
        mockMvc.perform(get("/api/admin/links/ZZZZZZZ/clicks"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LINK_NOT_FOUND"));
        mockMvc.perform(get("/ZZZZZZZ"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LINK_NOT_FOUND"));
    }

    @Test
    void actuatorHealthGroupsAndAdditionalProbePathsAreAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/livez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/readyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createResolveDuplicateDeactivateAndAnalyticsLifecycle() throws Exception {
        String firstDestination = "https://Example.com:443/path?q=1#fragment";
        JsonNode created = createLink(firstDestination, "attacker.example", 201);
        String code = created.get("code").asText();

        assertEquals("https://example.com/path?q=1", created.get("destination").asText());
        assertEquals("http://short.test/" + code, created.get("shortUrl").asText());

        JsonNode duplicate = createLink("https://example.com/path?q=1", null, 200);
        assertEquals(code, duplicate.get("code").asText());

        mockMvc.perform(get("/api/admin/links/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/admin/links/{code}/clicks", code))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.buckets.length()").value(0));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/path?q=1"))
                .andExpect(header().string("Cache-Control", "no-store"));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/path?q=1"))
                .andExpect(header().string("Cache-Control", "no-store"));

        mockMvc.perform(get("/api/admin/links/{code}/clicks", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.bucketUnit").value("minute"))
                .andExpect(jsonPath("$.buckets.length()").value(1));

        mockMvc.perform(post("/api/admin/links/{code}/deactivate", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/admin/links/{code}/deactivate", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("LINK_INACTIVE"));
        mockMvc.perform(get("/api/admin/links/{code}/clicks", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        JsonNode recreated = createLink("https://example.com/path?q=1", null, 201);
        assertNotEquals(code, recreated.get("code").asText());

        mockMvc.perform(get("/api/admin/links")
                        .param("limit", "100")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[?(@.code=='" + code + "')].active").value(false));
    }

    @Test
    void rolledBackDeactivateLeavesWarmCacheAndDatabaseActive() throws Exception {
        String code = linkService.create("https://example.com/rollback/" + UUID.randomUUID())
                .link()
                .getCode();
        assertTrue(linkService.resolveForRedirect(code).active());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            linkService.deactivate(code);
            throw new IllegalStateException("force rollback after deactivation");
        }));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Cache-Control", "no-store"));
        assertTrue(shortLinkRepository.findById(code).orElseThrow().isActive());
        assertEquals(1, analyticsService.stats(code).total());
    }

    @Test
    void acceptsAndDeduplicatesMaximumLengthDestination() throws Exception {
        String destination = "https://example.com/" + "a".repeat(2028);
        assertEquals(2048, destination.length());

        JsonNode created = createLink(destination, null, 201);
        JsonNode duplicate = createLink(destination, null, 200);

        assertEquals(destination, created.get("destination").asText());
        assertEquals(created.get("code").asText(), duplicate.get("code").asText());
    }

    @Test
    void cursorPagesAreStableAndRejectMalformedTokens() throws Exception {
        Set<String> expectedCodes = new HashSet<>();
        for (int index = 0; index < 5; index++) {
            expectedCodes.add(createLink("https://example.test/cursor/" + UUID.randomUUID(), null, 201)
                    .get("code")
                    .asText());
        }

        List<String> pageCodes = new ArrayList<>();
        String cursor = null;
        do {
            MockHttpServletRequestBuilder request = get("/api/admin/links")
                    .param("limit", "2")
                    .param("active", "true");
            if (cursor != null) {
                request.param("cursor", cursor);
            }
            JsonNode page = objectMapper.readTree(mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
            assertTrue(page.get("links").size() <= 2);
            for (JsonNode link : page.get("links")) {
                pageCodes.add(link.get("code").asText());
            }
            JsonNode nextCursor = page.get("nextCursor");
            cursor = nextCursor == null || nextCursor.isNull() ? null : nextCursor.asText();
        } while (cursor != null);

        assertEquals(expectedCodes, new HashSet<>(pageCodes));
        assertEquals(pageCodes.size(), new HashSet<>(pageCodes).size());

        mockMvc.perform(get("/api/admin/links").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    @Test
    void concurrentCreatesReuseTheSingleActiveDestination() throws Exception {
        int requestCount = 6;
        String destination = "https://example.com/concurrent/" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<LinkService.CreateOutcome>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent create");
                    }
                    return linkService.create(destination);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            List<LinkService.CreateOutcome> outcomes = new ArrayList<>();
            for (Future<LinkService.CreateOutcome> future : futures) {
                outcomes.add(future.get(20, TimeUnit.SECONDS));
            }

            assertEquals(1, outcomes.stream().map(outcome -> outcome.link().getCode()).distinct().count());
            assertEquals(1, outcomes.stream().filter(LinkService.CreateOutcome::newlyCreated).count());
            assertEquals(1, shortLinkRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentFirstClicksCreateOneBucketWithEveryClickCounted() throws Exception {
        int requestCount = 6;
        String code = linkService.create("https://example.com/click-race/" + UUID.randomUUID())
                .link()
                .getCode();
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent clicks");
                    }
                    analyticsService.incrementCurrentMinute(code);
                    return null;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }

            assertEquals(requestCount, analyticsService.stats(code).total());
            assertEquals(1, analyticsService.stats(code).buckets().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void fileStoreSurvivesApplicationContextRestart(@TempDir Path dataPath) {
        String[] arguments = {
            "--spring.main.banner-mode=off",
            "--app.data-path=" + dataPath,
            "--app.base-url=http://short.test"
        };
        String code;

        try (ConfigurableApplicationContext first = new SpringApplicationBuilder(UrlShortenerApplication.class)
                .web(WebApplicationType.NONE)
                .run(arguments)) {
            LinkService.CreateOutcome created = first.getBean(LinkService.class)
                    .create("https://example.com/restart/" + UUID.randomUUID());
            code = created.link().getCode();
            first.getBean(AnalyticsService.class).incrementCurrentMinute(code);
        }

        try (ConfigurableApplicationContext restarted = new SpringApplicationBuilder(UrlShortenerApplication.class)
                .web(WebApplicationType.NONE)
                .run(arguments)) {
            var link = restarted.getBean(LinkService.class).resolveForRedirect(code);
            assertTrue(link.active());
            assertEquals(1, restarted.getBean(AnalyticsService.class).stats(code).total());
        }
    }

    private JsonNode createLink(String destination, String host, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/admin/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("destination", destination)));
        if (host != null) {
            request.header("Host", host);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
