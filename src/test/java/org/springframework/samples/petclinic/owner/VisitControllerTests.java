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
package org.springframework.samples.petclinic.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(VisitController.class)
@DisabledInNativeImage
@DisabledInAotMode
class VisitControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	private static final String VISITS_URL = "/api/owners/{ownerId}/pets/{petId}/visits";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OwnerRepository owners;

	@BeforeEach
	void init() {
		Owner owner = new Owner();
		Pet pet = new Pet();
		owner.addPet(pet);
		pet.setId(TEST_PET_ID);
		Visit visit = new Visit();
		visit.setId(7);
		visit.setDescription("rabies shot");
		pet.addVisit(visit);
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
		// save() returns the owner it was given, after giving new visits an id
		given(this.owners.save(any(Owner.class))).willAnswer(invocation -> {
			Owner saved = invocation.getArgument(0);
			saved.getPet(TEST_PET_ID).getVisits().stream().filter(Visit::isNew).forEach(v -> v.setId(42));
			return saved;
		});
	}

	private MvcTestResult createVisit(String body) {
		return mvc.post()
			.uri(VISITS_URL, TEST_OWNER_ID, TEST_PET_ID)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	@Test
	void listVisits() {
		assertThat(mvc.get().uri(VISITS_URL, TEST_OWNER_ID, TEST_PET_ID)).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$[0].id", id -> assertThat(id).isEqualTo(7))
			.hasPathSatisfying("$[0].description", description -> assertThat(description).isEqualTo("rabies shot"));
	}

	@Test
	void showVisit() {
		assertThat(mvc.get().uri(VISITS_URL + "/{visitId}", TEST_OWNER_ID, TEST_PET_ID, 7)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.description")
			.isEqualTo("rabies shot");
	}

	@Test
	void showUnknownVisitReturnsNotFound() {
		assertThat(mvc.get().uri(VISITS_URL + "/{visitId}", TEST_OWNER_ID, TEST_PET_ID, 8))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void createVisitSuccess() {
		String tomorrow = LocalDate.now().plusDays(1).toString();
		assertThat(createVisit("""
				{"date": "%s", "description": "Visit Description"}
				""".formatted(tomorrow))).hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "http://localhost/api/owners/1/pets/1/visits/42")
			.bodyJson()
			.hasPathSatisfying("$.id", id -> assertThat(id).isEqualTo(42))
			.hasPathSatisfying("$.date", date -> assertThat(date).isEqualTo(tomorrow))
			.hasPathSatisfying("$.description", description -> assertThat(description).isEqualTo("Visit Description"));
	}

	@Test
	void createVisitDefaultsDateToTomorrow() {
		assertThat(createVisit("""
				{"description": "Visit Description"}
				""")).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.date")
			.isEqualTo(LocalDate.now().plusDays(1).toString());
	}

	@Test
	void createVisitHasErrorsWhenDescriptionIsMissing() {
		assertThat(createVisit("{}")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("description");
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void createVisitHasErrorsWhenVisitDateIsNotInFuture() {
		assertThat(createVisit("""
				{"date": "%s", "description": "Visit Description"}
				""".formatted(LocalDate.now()))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.hasPathSatisfying("$.errors[0].field", field -> assertThat(field).isEqualTo("date"))
			.hasPathSatisfying("$.errors[0].message",
					message -> assertThat(message).isEqualTo("Visit date must be in the future"));
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void createVisitHasErrorsWhenDescriptionIsTooLong() {
		assertThat(createVisit("""
				{"description": "%s"}
				""".formatted("a".repeat(256)))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("description");
	}

}
