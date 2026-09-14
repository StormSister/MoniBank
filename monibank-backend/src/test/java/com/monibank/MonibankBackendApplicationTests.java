package com.monibank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(properties = {
		"monibank.security.admin.username=test-admin",
		"monibank.security.admin.password=test-password",
		"monibank.security.admin.jwt-secret="
				+ "VGhpcy1pcy1hLXRlc3Qta2V5LXdpdGgtMzItYnl0ZXMhIQ=="
})
class MonibankBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
