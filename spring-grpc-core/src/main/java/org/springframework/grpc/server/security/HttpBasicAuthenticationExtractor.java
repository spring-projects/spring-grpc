/*
 * Copyright 2024-present the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Extracts the HTTP Basic authentication credentials from the gRPC request headers. If
 * the 'Authorization' header is present and starts with 'Basic ', the username and
 * password are extracted from the Base64-encoded header value and returned as a
 * {@link UsernamePasswordAuthenticationToken}. If the header is not present or does not
 * start with 'Basic ', this method returns null.
 *
 * @author Dave Syer
 */
public class HttpBasicAuthenticationExtractor implements GrpcAuthenticationExtractor {

	@Override
	public Authentication extract(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {
		String auth = headers.get(GrpcSecurity.AUTHORIZATION_KEY);
		if (auth == null) {
			return null;
		}
		if (!auth.toLowerCase(Locale.ROOT).startsWith("basic ")) {
			return null;
		}
		auth = auth.substring("basic ".length());
		return extract(auth);
	}

	private Authentication extract(String auth) {
		String[] parts = new String(Base64.getDecoder().decode(auth), StandardCharsets.UTF_8).split(":");
		if (parts.length != 2) {
			return null;
		}
		return new UsernamePasswordAuthenticationToken(parts[0], parts[1]);
	}

}
