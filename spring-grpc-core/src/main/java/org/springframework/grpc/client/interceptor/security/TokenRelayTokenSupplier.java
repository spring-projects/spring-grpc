/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
