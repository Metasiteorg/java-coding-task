/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.system;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Integration Test for {@link CrashController}.
 *
 * @author Alex Lutz
 */
// NOT Waiting https://github.com/spring-projects/spring-boot/issues/5574
@SpringBootTest(webEnvironment = RANDOM_PORT,
		properties = { "spring.web.error.include-message=ALWAYS", "management.endpoints.access.default=none" })
@AutoConfigureRestTestClient
class CrashControllerIntegrationTests {

	@Autowired
	private RestTestClient client;

	@Test
	void triggerExceptionJson() {
		client.get()
			.uri("/api/oups")
			.exchange()
			.expectStatus()
			.isEqualTo(500)
			.expectBody()
			.jsonPath("$.timestamp")
			.exists()
			.jsonPath("$.status")
			.isEqualTo(500)
			.jsonPath("$.error")
			.exists()
			.jsonPath("$.message")
			.isEqualTo("Expected: controller used to showcase what happens when an exception is thrown")
			.jsonPath("$.path")
			.isEqualTo("/api/oups");
	}

	@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
	static class TestConfiguration {

	}

}
