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

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for {@link Owner owners}.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Wick Dynex
 */
@RestController
@RequestMapping("/api/owners")
@Tag(name = "Owners")
class OwnerController {

	private final OwnerRepository owners;

	OwnerController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping
	@Operation(summary = "Find owners whose last name starts with the given text")
	Page<Owner> findOwners(@RequestParam(defaultValue = "") String lastName,
			@PageableDefault(size = 5) Pageable pageable) {
		return this.owners.findByLastNameStartingWith(lastName.strip(), pageable);
	}

	@GetMapping("/{ownerId}")
	@Operation(summary = "Get an owner with their pets and visits")
	Owner showOwner(@PathVariable int ownerId) {
		return findOwner(ownerId);
	}

	@PostMapping
	@Operation(summary = "Create an owner")
	ResponseEntity<Owner> createOwner(@Valid @RequestBody OwnerRequest request) {
		Owner owner = new Owner();
		request.applyTo(owner);
		Owner saved = this.owners.save(owner);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{ownerId}")
			.buildAndExpand(saved.getId())
			.toUri();
		return ResponseEntity.created(location).body(saved);
	}

	@PutMapping("/{ownerId}")
	@Operation(summary = "Update an owner's details")
	Owner updateOwner(@PathVariable int ownerId, @Valid @RequestBody OwnerRequest request) {
		Owner owner = findOwner(ownerId);
		request.applyTo(owner);
		return this.owners.save(owner);
	}

	private Owner findOwner(int ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner " + ownerId + " not found"));
	}

}
