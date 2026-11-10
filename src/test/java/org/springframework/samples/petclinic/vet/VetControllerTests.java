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
package org.springframework.samples.petclinic.vet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Test class for the {@link VetController}
 */
@WebMvcTest(VetController.class)
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private VetRepository vets;

	private Vet james() {
		Vet james = new Vet();
		james.setFirstName("James");
		james.setLastName("Carter");
		james.setId(1);
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setFirstName("Helen");
		helen.setLastName("Leary");
		helen.setId(2);
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		helen.addSpecialty(radiology);
		return helen;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(List.of(james(), helen()));
		given(this.vets.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(james(), helen())));
	}

	@Test
	void showVetList() {
		assertThat(mvc.get().uri("/api/vets").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.hasContentType(MediaType.APPLICATION_JSON)
			.bodyJson()
			.hasPathSatisfying("$[0].id", id -> assertThat(id).isEqualTo(1))
			.hasPathSatisfying("$[1].specialties[0].name", name -> assertThat(name).isEqualTo("radiology"));
	}

	@Test
	void showVetPage() {
		assertThat(mvc.get().uri("/api/vets/paged").param("page", "0")).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$.content[0].lastName", lastName -> assertThat(lastName).isEqualTo("Carter"))
			.hasPathSatisfying("$.page.totalElements", total -> assertThat(total).isEqualTo(2));
	}

}
