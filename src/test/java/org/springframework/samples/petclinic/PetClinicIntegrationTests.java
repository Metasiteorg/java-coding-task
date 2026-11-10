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

package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "logging.level.sql=DEBUG")
@AutoConfigureRestTestClient
public class PetClinicIntegrationTests {

	@Autowired
	private VetRepository vets;

	@Autowired
	private RestTestClient client;

	@Test
	void findAll() {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		client.get().uri("/api/owners/1").exchange().expectStatus().isOk();
	}

	@Test
	void ownerList() {
		client.get().uri("/api/owners?lastName=").exchange().expectStatus().isOk();
	}

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, "--spring.docker.compose.lifecycle-management=NONE");
	}

}
