/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;

public abstract class AbstractGrpcClientRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public final void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
		GrpcClientRegistrationSpec[] specs = collect(meta);
		String name = GrpcClientFactory.class.getName();
		for (GrpcClientRegistrationSpec spec : specs) {
			GrpcClientFactory.register(registry, spec);
		}
		if (!registry.containsBeanDefinition(name)) {
			registry.registerBeanDefinition(name, new RootBeanDefinition(GrpcClientFactory.class));
		}
	}

	protected abstract GrpcClientRegistrationSpec[] collect(AnnotationMetadata meta);

}
