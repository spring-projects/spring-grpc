/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
