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
