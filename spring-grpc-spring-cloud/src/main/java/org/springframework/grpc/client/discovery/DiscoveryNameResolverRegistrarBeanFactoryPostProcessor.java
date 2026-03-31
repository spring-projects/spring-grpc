/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.grpc.client.discovery;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * Ensures {@link DiscoveryNameResolverRegistrar} is initialized before any
 * {@link GrpcChannelFactory} bean.
 *
 * @author Hyacinth Contributor
 */
public class DiscoveryNameResolverRegistrarBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private final String registrarBeanName;

	public DiscoveryNameResolverRegistrarBeanFactoryPostProcessor(String registrarBeanName) {
		this.registrarBeanName = registrarBeanName;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanNames = beanFactory.getBeanNamesForType(GrpcChannelFactory.class, false, false);
		for (String beanName : beanNames) {
			if (!beanFactory.containsBeanDefinition(beanName)) {
				continue;
			}
			AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(beanName);
			Set<String> dependsOn = new LinkedHashSet<>();
			String[] existingDependsOn = beanDefinition.getDependsOn();
			if (existingDependsOn != null) {
				dependsOn.addAll(Set.of(existingDependsOn));
			}
			dependsOn.add(this.registrarBeanName);
			beanDefinition.setDependsOn(dependsOn.toArray(String[]::new));
		}
	}

}
