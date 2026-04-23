package com.northcare.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ServiceDiscoveryApplicationTest {

    @Test
    void contextLoads() {
        // Verifies Eureka server starts up correctly
    }
}
