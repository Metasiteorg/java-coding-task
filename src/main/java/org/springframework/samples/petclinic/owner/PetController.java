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
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for an owner's {@link Pet pets}.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 */
@RestController
@Tag(name = "Pets")
class PetController {

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	PetController(OwnerRepository owners, PetTypeRepository types) {
		this.owners = owners;
		this.types = types;
	}

	@GetMapping("/api/pettypes")
	@Operation(summary = "List all pet types")
	List<PetType> listPetTypes() {
		return this.types.findPetTypes();
	}

	@GetMapping("/api/owners/{ownerId}/pets/{petId}")
	@Operation(summary = "Get a pet with its visits")
	Pet showPet(@PathVariable int ownerId, @PathVariable int petId) {
		return findPet(findOwner(ownerId), petId);
	}

	@PostMapping("/api/owners/{ownerId}/pets")
	@Operation(summary = "Add a pet to an owner")
	ResponseEntity<Pet> createPet(@PathVariable int ownerId, @Valid @RequestBody PetRequest petRequest) {
		Owner owner = findOwner(ownerId);
		if (owner.getPet(petRequest.name(), true) != null) {
			throw duplicateName(petRequest.name());
		}

		Pet pet = new Pet();
		applyTo(pet, petRequest);
		owner.addPet(pet);

		Owner saved = saveOwner(owner, petRequest.name());
		Pet created = saved.getPet(petRequest.name(), true);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{petId}")
			.buildAndExpand(created.getId())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/api/owners/{ownerId}/pets/{petId}")
	@Operation(summary = "Update a pet's details")
	Pet updatePet(@PathVariable int ownerId, @PathVariable int petId, @Valid @RequestBody PetRequest petRequest) {
		Owner owner = findOwner(ownerId);
		Pet pet = findPet(owner, petId);

		Pet sameName = owner.getPet(petRequest.name(), false);
		if (sameName != null && !Objects.equals(sameName.getId(), pet.getId())) {
			throw duplicateName(petRequest.name());
		}

		applyTo(pet, petRequest);
		return saveOwner(owner, petRequest.name()).getPet(petId);
	}

	private void applyTo(Pet pet, PetRequest petRequest) {
		pet.setName(petRequest.name());
		pet.setBirthDate(petRequest.birthDate());
		pet.setType(this.types.findByName(petRequest.type())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unknown pet type: " + petRequest.type())));
	}

	private Owner saveOwner(Owner owner, String petName) {
		try {
			return this.owners.saveAndFlush(owner);
		}
		catch (DataIntegrityViolationException ex) {
			if (!isDuplicatePetNameViolation(ex)) {
				throw ex;
			}
			throw duplicateName(petName);
		}
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

	private ResponseStatusException duplicateName(String petName) {
		return new ResponseStatusException(HttpStatus.CONFLICT, "Pet name '" + petName + "' is already in use");
	}

	private boolean isDuplicatePetNameViolation(DataIntegrityViolationException ex) {
		String message = ex.getMessage();
		return message != null && message.toLowerCase().contains("unique_owner_pet_name");
	}

}
