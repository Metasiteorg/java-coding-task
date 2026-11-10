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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(OwnerController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final String VALID_OWNER = """
			{"firstName": "Joe", "lastName": "Bloggs", "address": "123 Caramel Street",
			 "city": "London", "telephone": "1316761638"}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner george() {
		Owner george = new Owner();
		george.setId(TEST_OWNER_ID);
		george.setFirstName("George");
		george.setLastName("Franklin");
		george.setAddress("110 W. Liberty St.");
		george.setCity("Madison");
		george.setTelephone("6085551023");
		Pet max = new Pet();
		PetType dog = new PetType();
		dog.setName("dog");
		max.setType(dog);
		max.setName("Max");
		max.setBirthDate(LocalDate.now());
		george.addPet(max);
		max.setId(1);
		Visit visit = new Visit();
		visit.setDate(LocalDate.now());
		max.addVisit(visit);
		return george;
	}

	@BeforeEach
	void setup() {
		given(this.owners.findByLastNameStartingWith(eq("Franklin"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george())));
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george()));
		given(this.owners.save(any(Owner.class))).willAnswer(invocation -> {
			Owner saved = invocation.getArgument(0);
			if (saved.isNew()) {
				saved.setId(11);
			}
			return saved;
		});
	}

	@Test
	void createOwnerSuccess() {
		assertThat(mvc.post().uri("/api/owners").contentType(MediaType.APPLICATION_JSON).content(VALID_OWNER))
			.hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "http://localhost/api/owners/11")
			.bodyJson()
			.hasPathSatisfying("$.id", id -> assertThat(id).isEqualTo(11))
			.hasPathSatisfying("$.lastName", lastName -> assertThat(lastName).isEqualTo("Bloggs"))
			.doesNotHavePath("$.new");
	}

	@Test
	void createOwnerIgnoresClientSuppliedId() {
		assertThat(mvc.post()
			.uri("/api/owners")
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_OWNER.replace("{", "{\"id\": 1, "))).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.id")
			.isEqualTo(11);
	}

	@Test
	void createOwnerHasErrors() {
		assertThat(mvc.post().uri("/api/owners").contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Joe", "lastName": "Bloggs", "city": "London"}
				""")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactlyInAnyOrder("address", "telephone");
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void createOwnerRejectsCityLongerThanColumn() {
		assertThat(mvc.post()
			.uri("/api/owners")
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_OWNER.replace("London", "C".repeat(81)))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("city");
	}

	@Test
	void createOwnerRejectsInvalidTelephoneWithLocalizedMessage() {
		assertThat(mvc.post()
			.uri("/api/owners")
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_OWNER.replace("1316761638", "12"))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.hasPathSatisfying("$.errors[0].field", field -> assertThat(field).isEqualTo("telephone"))
			.hasPathSatisfying("$.errors[0].message",
					message -> assertThat(message).isEqualTo("Telephone must be a 10-digit number"));
	}

	@Test
	void findOwnersByLastName() {
		assertThat(mvc.get().uri("/api/owners").param("lastName", "Franklin")).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$.content[0].id", id -> assertThat(id).isEqualTo(TEST_OWNER_ID))
			.hasPathSatisfying("$.page.totalElements", total -> assertThat(total).isEqualTo(1));
	}

	@Test
	void findOwnersIgnoresSurroundingWhitespace() {
		for (String lastName : List.of(" Franklin", "Franklin ", " Franklin ")) {
			assertThat(mvc.get().uri("/api/owners").param("lastName", lastName)).hasStatusOk()
				.bodyJson()
				.extractingPath("$.content[0].lastName")
				.isEqualTo("Franklin");
		}
		verify(this.owners, times(3)).findByLastNameStartingWith(eq("Franklin"), any(Pageable.class));
	}

	@Test
	void findOwnersWithoutLastNameReturnsAllOwners() {
		given(this.owners.findByLastNameStartingWith(eq(""), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george(), new Owner())));
		assertThat(mvc.get().uri("/api/owners")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.content")
			.asArray()
			.hasSize(2);
	}

	@Test
	void findOwnersUsesRequestedPage() {
		given(this.owners.findByLastNameStartingWith(eq(""), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of()));
		assertThat(mvc.get().uri("/api/owners").param("page", "2")).hasStatusOk();
		verify(this.owners).findByLastNameStartingWith(eq(""), eq(PageRequest.of(2, 5)));
	}

	@Test
	void showOwner() {
		assertThat(mvc.get().uri("/api/owners/{ownerId}", TEST_OWNER_ID)).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$.lastName", value -> assertThat(value).isEqualTo("Franklin"))
			.hasPathSatisfying("$.firstName", value -> assertThat(value).isEqualTo("George"))
			.hasPathSatisfying("$.address", value -> assertThat(value).isEqualTo("110 W. Liberty St."))
			.hasPathSatisfying("$.city", value -> assertThat(value).isEqualTo("Madison"))
			.hasPathSatisfying("$.telephone", value -> assertThat(value).isEqualTo("6085551023"))
			.hasPathSatisfying("$.pets[0].name", value -> assertThat(value).isEqualTo("Max"))
			.hasPathSatisfying("$.pets[0].visits", visits -> assertThat(visits).asList().hasSize(1));
	}

	@Test
	void showUnknownOwnerReturnsNotFound() {
		assertThat(mvc.get().uri("/api/owners/{ownerId}", 99)).hasStatus(HttpStatus.NOT_FOUND)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Owner 99 not found");
	}

	@Test
	void updateOwnerSuccess() {
		assertThat(mvc.put()
			.uri("/api/owners/{ownerId}", TEST_OWNER_ID)
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_OWNER)).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$.id", id -> assertThat(id).isEqualTo(TEST_OWNER_ID))
			.hasPathSatisfying("$.lastName", lastName -> assertThat(lastName).isEqualTo("Bloggs"))
			.hasPathSatisfying("$.pets[0].name", name -> assertThat(name).isEqualTo("Max"));
	}

	@Test
	void updateOwnerHasErrors() {
		assertThat(mvc.put()
			.uri("/api/owners/{ownerId}", TEST_OWNER_ID)
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_OWNER.replace("123 Caramel Street", ""))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("address");
		verify(this.owners, never()).save(any(Owner.class));
	}

}
