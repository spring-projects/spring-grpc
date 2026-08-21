/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.grpc.server.security;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * A {@link GrpcAuthenticationExtractor} that iterates over other
 * {@link GrpcAuthenticationExtractor} instances in the order supplied. The first non-null
 * {@link Authentication} is the result, and the remaining delegates are not called. An
 * exception from a delegate is not caught.
 *
 * @author Gabriel Hall
 */
public final class DelegatingGrpcAuthenticationExtractor implements GrpcAuthenticationExtractor {

	private final List<GrpcAuthenticationExtractor> delegates;

	public DelegatingGrpcAuthenticationExtractor(List<GrpcAuthenticationExtractor> delegates) {
		Assert.notEmpty(delegates, "delegates cannot be empty");
		this.delegates = List.copyOf(delegates);
	}

	public DelegatingGrpcAuthenticationExtractor(GrpcAuthenticationExtractor... delegates) {
		Assert.notEmpty(delegates, "delegates cannot be empty");
		this.delegates = List.of(delegates);
	}

	@Override
	public @Nullable Authentication extract(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {
		for (GrpcAuthenticationExtractor delegate : this.delegates) {
			Authentication authentication = delegate.extract(headers, attributes, method);
			if (authentication != null) {
				return authentication;
			}
		}
		return null;
	}

}
