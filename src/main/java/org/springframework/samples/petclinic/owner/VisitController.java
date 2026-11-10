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
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for a pet's {@link Visit visits}.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 * @author Wick Dynex
 */
@RestController
@RequestMapping("/api/owners/{ownerId}/pets/{petId}/visits")
@Tag(name = "Visits")
class VisitController {

	private final OwnerRepository owners;

	VisitController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping
	@Operation(summary = "List a pet's visits")
	Collection<Visit> listVisits(@PathVariable int ownerId, @PathVariable int petId) {
		return findPet(findOwner(ownerId), petId).getVisits();
	}

	@GetMapping("/{visitId}")
	@Operation(summary = "Get one visit")
	Visit showVisit(@PathVariable int ownerId, @PathVariable int petId, @PathVariable int visitId) {
		return findPet(findOwner(ownerId), petId).getVisits()
			.stream()
			.filter(visit -> Objects.equals(visit.getId(), visitId))
			.findFirst()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Visit " + visitId + " not found for pet " + petId));
	}

	@PostMapping
	@Operation(summary = "Book a visit for a pet")
	ResponseEntity<Visit> createVisit(@PathVariable int ownerId, @PathVariable int petId,
			@Valid @RequestBody VisitRequest visitRequest) {
		Owner owner = findOwner(ownerId);
		Set<Integer> existingVisitIds = findPet(owner, petId).getVisits()
			.stream()
			.map(Visit::getId)
			.collect(Collectors.toSet());

		Visit visit = new Visit();
		visit.setDate(Objects.requireNonNullElseGet(visitRequest.date(), () -> LocalDate.now().plusDays(1)));
		visit.setDescription(visitRequest.description());
		owner.addVisit(petId, visit);
		Owner saved = this.owners.save(owner);

		// save() merges the detached owner, so the persisted visit is a new instance
		Visit created = saved.getPet(petId)
			.getVisits()
			.stream()
			.filter(v -> !existingVisitIds.contains(v.getId()))
			.findFirst()
			.orElseThrow();
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{visitId}")
			.buildAndExpand(created.getId())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	private Owner findOwner(int ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner " + ownerId + " not found"));
	}

	private Pet findPet(Owner owner, int petId) {
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Pet " + petId + " not found for owner " + owner.getId());
		}
		return pet;
	}

}
