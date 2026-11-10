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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Test class for the {@link PetController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(PetController.class)
@DisabledInNativeImage
@DisabledInAotMode
class PetControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository types;

	@BeforeEach
	void setup() {
		PetType hamster = new PetType();
		hamster.setId(3);
		hamster.setName("hamster");
		given(this.types.findPetTypes()).willReturn(List.of(hamster));
		given(this.types.findByName("hamster")).willReturn(Optional.of(hamster));

		Owner owner = new Owner();
		Pet pet = new Pet();
		Pet dog = new Pet();
		owner.addPet(pet);
		owner.addPet(dog);
		pet.setId(TEST_PET_ID);
		dog.setId(TEST_PET_ID + 1);
		pet.setName("petty");
		dog.setName("doggy");
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
		given(this.owners.saveAndFlush(any(Owner.class))).willAnswer(invocation -> {
			Owner saved = invocation.getArgument(0);
			saved.getPets().stream().filter(Pet::isNew).forEach(p -> p.setId(99));
			return saved;
		});
	}

	private MvcTestResult createPet(String body) {
		return mvc.post()
			.uri("/api/owners/{ownerId}/pets", TEST_OWNER_ID)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private MvcTestResult updatePet(int petId, String body) {
		return mvc.put()
			.uri("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, petId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private static String pet(String name, String type, String birthDate) {
		return """
				{"name": "%s", "type": "%s", "birthDate": "%s"}
				""".formatted(name, type, birthDate);
	}

	@Test
	void listPetTypes() {
		assertThat(mvc.get().uri("/api/pettypes")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[0].name")
			.isEqualTo("hamster");
	}

	@Test
	void showPet() {
		assertThat(mvc.get().uri("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, TEST_PET_ID)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.name")
			.isEqualTo("petty");
	}

	@Test
	void showUnknownPetReturnsNotFound() {
		assertThat(mvc.get().uri("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, 42))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void createPetSuccess() {
		assertThat(createPet(pet("Betty", "hamster", "2015-02-12"))).hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "http://localhost/api/owners/1/pets/99")
			.bodyJson()
			.hasPathSatisfying("$.id", id -> assertThat(id).isEqualTo(99))
			.hasPathSatisfying("$.type.name", type -> assertThat(type).isEqualTo("hamster"));
	}

	@Test
	void createPetForUnknownOwnerReturnsNotFound() {
		assertThat(mvc.post()
			.uri("/api/owners/{ownerId}/pets", 99)
			.contentType(MediaType.APPLICATION_JSON)
			.content(pet("Betty", "hamster", "2015-02-12"))).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Nested
	class CreatePetHasErrors {

		@Test
		void createPetWithBlankName() {
			assertThat(createPet(pet("\\t \\n", "hamster", "2015-02-12"))).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.hasPathSatisfying("$.errors[0].field", field -> assertThat(field).isEqualTo("name"))
				.hasPathSatisfying("$.errors[0].message", message -> assertThat(message).isEqualTo("is required"));
		}

		@Test
		void createPetWithTooLongName() {
			assertThat(createPet(pet("B".repeat(31), "hamster", "2015-02-12"))).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("name");
		}

		@Test
		void createPetWithDuplicateName() {
			assertThat(createPet(pet("petty", "hamster", "2015-02-12"))).hasStatus(HttpStatus.CONFLICT);
			verify(owners, never()).saveAndFlush(any(Owner.class));
		}

		@Test
		void createPetWithMissingPetType() {
			assertThat(createPet("""
					{"name": "Betty", "birthDate": "2015-02-12"}
					""")).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("type");
		}

		@Test
		void createPetWithUnknownPetType() {
			assertThat(createPet(pet("Betty", "dragon", "2015-02-12"))).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.detail")
				.isEqualTo("Unknown pet type: dragon");
		}

		@Test
		void createPetWithMissingBirthDate() {
			assertThat(createPet("""
					{"name": "Betty", "type": "hamster"}
					""")).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("birthDate");
		}

		@Test
		void createPetWithFutureBirthDate() {
			String futureBirthDate = LocalDate.now().plusMonths(1).toString();
			assertThat(createPet(pet("Betty", "hamster", futureBirthDate))).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.hasPathSatisfying("$.errors[0].field", field -> assertThat(field).isEqualTo("birthDate"))
				.hasPathSatisfying("$.errors[0].message", message -> assertThat(message).isEqualTo("invalid date"));
		}

		@Test
		void createPetWithMalformedBirthDate() {
			assertThat(createPet(pet("Betty", "hamster", "2015/02/12"))).hasStatus(HttpStatus.BAD_REQUEST);
		}

		@Test
		void createPetWithDataIntegrityViolation() {
			given(owners.saveAndFlush(any(Owner.class)))
				.willThrow(new DataIntegrityViolationException("Duplicate key: unique_owner_pet_name"));
			assertThat(createPet(pet("Betty", "hamster", "2015-02-12"))).hasStatus(HttpStatus.CONFLICT);
		}

	}

	@Test
	void updatePetSuccess() {
		assertThat(updatePet(TEST_PET_ID, pet("Betty", "hamster", "2015-02-12"))).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$.id", id -> assertThat(id).isEqualTo(TEST_PET_ID))
			.hasPathSatisfying("$.name", name -> assertThat(name).isEqualTo("Betty"));
	}

	@Test
	void updatePetWithSameName() {
		assertThat(updatePet(TEST_PET_ID, pet("petty", "hamster", "2015-02-12"))).hasStatusOk();
	}

	@Nested
	class UpdatePetHasErrors {

		@Test
		void updatePetWithDuplicateName() {
			assertThat(updatePet(TEST_PET_ID + 1, pet("petty", "hamster", "2015-02-12")))
				.hasStatus(HttpStatus.CONFLICT);
		}

		@Test
		void updatePetWithBlankName() {
			assertThat(updatePet(TEST_PET_ID, pet("  ", "hamster", "2015-02-12"))).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("name");
		}

		@Test
		void updatePetWithDataIntegrityViolation() {
			given(owners.saveAndFlush(any(Owner.class)))
				.willThrow(new DataIntegrityViolationException("Duplicate key: unique_owner_pet_name"));
			assertThat(updatePet(TEST_PET_ID, pet("Betty", "hamster", "2015-02-12"))).hasStatus(HttpStatus.CONFLICT);
		}

	}

}
