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

package org.springframework.grpc.internal;

import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * A custom implementation of the TrustManagerFactory class that provides an insecure
 * trust manager. This trust manager does not perform any certificate validation and
 * accepts all certificates. It is intended for testing or development purposes only and
 * should not be used in production environments.
 *
 * @author David Syer
 */
public class InsecureTrustManagerFactory extends TrustManagerFactory {

	/** Single instance of the factory. */
	public static final TrustManagerFactory INSTANCE = new InsecureTrustManagerFactory();

	private static final Provider provider = new Provider("", "0.0", "") {
		private static final long serialVersionUID = -2680540247105807895L;

	};

	protected InsecureTrustManagerFactory() {
		super(new SimpleTrustManagerFactorySpi(), provider, "");
	}

	private final static class InsecureTrustManager extends X509ExtendedTrustManager {

		static final InsecureTrustManager INSTANCE = new InsecureTrustManager();

		static final X509Certificate[] EMPTY_CERTS = new X509Certificate[] {};

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return EMPTY_CERTS;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
		}

	}

	private final static class SimpleTrustManagerFactorySpi extends TrustManagerFactorySpi {

		static final TrustManager[] TRUST_ALL = new X509TrustManager[] { InsecureTrustManager.INSTANCE };

		@Override
		protected void engineInit(KeyStore keyStore) throws KeyStoreException {
		}

		@Override
		protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
				throws InvalidAlgorithmParameterException {
		}

		@Override
		protected TrustManager[] engineGetTrustManagers() {
			return TRUST_ALL;
		}

	}

}
