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

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns validation failures into RFC 9457 problem details with an {@code errors} list,
 * one entry per invalid field. Messages come from {@code messages*.properties} using the
 * request's {@code Accept-Language}.
 */
@RestControllerAdvice
class RestExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail body = ex.getBody();
		body.setProperty("errors", fieldErrors(ex.getBindingResult()));
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private List<FieldErrorDetail> fieldErrors(BindingResult result) {
		Locale locale = LocaleContextHolder.getLocale();
		MessageSource messageSource = getMessageSource();
		return result.getFieldErrors()
			.stream()
			.map(error -> new FieldErrorDetail(error.getField(), message(error, messageSource, locale)))
			.toList();
	}

	private static String message(FieldError error, MessageSource messageSource, Locale locale) {
		return messageSource != null ? messageSource.getMessage(error, locale) : error.getDefaultMessage();
	}

	record FieldErrorDetail(String field, String message) {

	}

}
