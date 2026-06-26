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

package org.springframework.grpc.client.interceptor.security;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

/**
 * A supplier that provides OAuth2 access tokens using the client credentials grant type.
 * This supplier retrieves the client registration from the provided repository and uses
 * it to request an access token from the authorization server.
 *
 * @author Dave Syer
 */
public class TokenRelayTokenSupplier implements TokenSupplier {

	private final OAuth2AuthorizedClientManager manager;

	public TokenRelayTokenSupplier(OAuth2AuthorizedClientManager manager) {
		this.manager = manager;
	}

	@Override
	public String token() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new BadCredentialsException("Unauthenticated");
		}
		if (!authentication.isAuthenticated()) {
			throw new AccessDeniedException("Authentication is not authenticated");
		}
		if (authentication instanceof OAuth2AuthenticationToken token) {
			OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
				.withClientRegistrationId(token.getAuthorizedClientRegistrationId())
				.principal(authentication)
				.build();
			return Objects.requireNonNull(this.manager.authorize(request)).getAccessToken().getTokenValue();
		}
		throw new BadCredentialsException("Wrong authentication type");
	}

}
