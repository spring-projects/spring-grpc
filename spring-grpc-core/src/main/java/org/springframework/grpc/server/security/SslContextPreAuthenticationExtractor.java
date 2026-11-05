/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
import org.springframework.security.web.authentication.preauth.x509.SubjectX500PrincipalExtractor;
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
		this(new SubjectX500PrincipalExtractor());
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
				X509Certificate certificate = Objects.requireNonNull(certificates[0], "certificate must not be null");
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
