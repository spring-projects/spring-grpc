/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
