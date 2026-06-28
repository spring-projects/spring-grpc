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

import java.util.function.Supplier;

import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * A supplier that provides OAuth2 access tokens using the client credentials grant type.
 * This supplier retrieves the client registration from the provided repository and uses
 * it to request an access token from the authorization server.
 *
 * @author Dave Syer
 */
public class ClientCredentialsTokenSupplier implements TokenSupplier {

	private final ClientRegistrationRepository repository;

	private final Supplier<String> clientId;

	public ClientCredentialsTokenSupplier(ClientRegistrationRepository repository, Supplier<String> clientId) {
		this.repository = repository;
		this.clientId = clientId;
	}

	@Override
	public String token() {
		RestClientClientCredentialsTokenResponseClient creds = new RestClientClientCredentialsTokenResponseClient();
		ClientRegistration reg = this.repository.findByRegistrationId(this.clientId.get());
		if (reg == null) {
			throw new IllegalArgumentException("No client registration found for id: " + this.clientId.get());
		}
		return creds.getTokenResponse(new OAuth2ClientCredentialsGrantRequest(reg)).getAccessToken().getTokenValue();
	}

}
