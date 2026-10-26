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

package org.springframework.grpc.server.scope;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers the {@value GrpcRequestScope#SCOPE_NAME} scope with the bean factory and the
 * global server interceptor that opens it for every call.
 *
 * @author Fernando Rodrigues Ripol
 * @author Sharang Gupta
 * @see EnableGrpcScope
 */
public class GrpcScopeRegistrar implements ImportBeanDefinitionRegistrar {

	static final String SCOPE_CONFIGURER_BEAN_NAME = "grpcRequestScopeConfigurer";

	static final String INTERCEPTOR_BEAN_NAME = "grpcRequestScopeServerInterceptor";

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(SCOPE_CONFIGURER_BEAN_NAME)) {
			RootBeanDefinition configurer = new RootBeanDefinition(CustomScopeConfigurer.class);
			configurer.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			configurer.getPropertyValues().add("scopes", Map.of(GrpcRequestScope.SCOPE_NAME, new GrpcRequestScope()));
			registry.registerBeanDefinition(SCOPE_CONFIGURER_BEAN_NAME, configurer);
		}
		if (!registry.containsBeanDefinition(INTERCEPTOR_BEAN_NAME)) {
			RootBeanDefinition interceptor = new RootBeanDefinition(GrpcRequestScopeServerInterceptor.class);
			interceptor.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(INTERCEPTOR_BEAN_NAME, interceptor);
		}
	}

}
