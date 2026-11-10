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

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoints for {@link Vet vets}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@RestController
@RequestMapping("/api/vets")
@Tag(name = "Vets")
class VetController {

	private final VetRepository vetRepository;

	VetController(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	@GetMapping
	@Operation(summary = "List all vets with their specialties")
	Collection<Vet> showVetList() {
		return this.vetRepository.findAll();
	}

	@GetMapping("/paged")
	@Operation(summary = "List vets one page at a time")
	Page<Vet> showVetPage(@PageableDefault(size = 5) Pageable pageable) {
		return this.vetRepository.findAll(pageable);
	}

}
