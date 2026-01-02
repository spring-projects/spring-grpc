/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.env.Environment;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.util.ClassUtils;

/**
 * {@link FailureAnalyzer} for missing {@link GrpcSecurity} in servlet-based gRPC
 * applications.
 *
 * @author Andrey Litvitski
 */
public class GrpcSecurityInServletFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException> {

	private static final String GRPC_SECURITY_PATH = "org.springframework.grpc.server.security.GrpcSecurity";

	private static final String GRPC_SERVLET_PATH = "io.grpc.servlet.jakarta.GrpcServlet";

	private final Environment environment;

	public GrpcSecurityInServletFailureAnalyzer(Environment environment) {
		this.environment = environment;
	}

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
		if (!isMissingGrpcSecurity(cause) || !isActuallyServletMode()) {
			return null;
		}
		return new FailureAnalysis(getDescription(), getAction(), cause);
	}

	private boolean isMissingGrpcSecurity(NoSuchBeanDefinitionException ex) {
		Class<?> type = ex.getBeanType();
		return type != null && GRPC_SECURITY_PATH.equals(type.getName());
	}

	private boolean isActuallyServletMode() {
		boolean servletOnClassPath = ClassUtils.isPresent(GRPC_SERVLET_PATH, getClass().getClassLoader());
		if (!servletOnClassPath) {
			return false;
		}
		String servletPropertyName = "spring.grpc.server.servlet.enabled";
		return this.environment.getProperty(servletPropertyName, Boolean.class, true);
	}

	private String getDescription() {
		return """
				GrpcSecurity is not available because this application is running the gRPC server in servlet mode (GrpcServlet).
				""";
	}

	private String getAction() {
		return """
				Configure security using the standard Spring Security servlet configuration (SecurityFilterChain / HttpSecurity).
				""";
	}

}
