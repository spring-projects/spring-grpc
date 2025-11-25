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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.util.Assert;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class SslContextPreAuthenticationExtractor implements GrpcAuthenticationExtractor {

	private static final Log logger = LogFactory.getLog(SslContextPreAuthenticationExtractor.class);

	private X509PrincipalExtractor principalExtractor;

	public SslContextPreAuthenticationExtractor() {
		this(new SubjectDnX509PrincipalExtractor());
	}

	public SslContextPreAuthenticationExtractor(X509PrincipalExtractor principalExtractor) {
		this.principalExtractor = principalExtractor;
	}

	@Override
	public @Nullable Authentication extract(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {
		SSLSession session = attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
		if (session != null) {
			@Nullable
			X509Certificate[] certificates = initCertificates(session);
			if (certificates != null) {
				Assert.notEmpty(certificates, "Must contain at least 1 non-null certificate");
				X509Certificate certificate = Objects.requireNonNull(certificates[0], "Certificate must not be null");
				return new PreAuthenticatedAuthenticationToken(this.principalExtractor.extractPrincipal(certificate),
						certificate);
			}
		}
		return null;
	}

	@SuppressWarnings("NullAway")
	@Nullable
	private static X509Certificate[] initCertificates(SSLSession session) {
		Certificate[] certificates;
		try {
			certificates = session.getPeerCertificates();
		}
		catch (Throwable ex) {
			logger.trace("Failed to get peer cert from session", ex);
			return null;
		}

		List<X509Certificate> result = new ArrayList<>(certificates.length);
		for (Certificate certificate : certificates) {
			if (certificate instanceof X509Certificate x509Certificate) {
				result.add(x509Certificate);
			}
		}
		return (!result.isEmpty() ? result.toArray(new X509Certificate[0]) : null);
	}

}
