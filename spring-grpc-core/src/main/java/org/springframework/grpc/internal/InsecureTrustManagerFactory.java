/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
