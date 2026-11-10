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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Metadata shown at the top of Swagger UI ({@code /swagger-ui.html}) and in the OpenAPI
 * document ({@code /v3/api-docs}).
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

	@Bean
	OpenAPI petClinicOpenApi() {
		return new OpenAPI().info(new Info().title("PetClinic REST API")
			.description("Owners, pets, visits and vets of the PetClinic sample application.")
			.version("v1")
			.license(new License().name("Apache License, Version 2.0")
				.url("https://www.apache.org/licenses/LICENSE-2.0")));
	}

}
