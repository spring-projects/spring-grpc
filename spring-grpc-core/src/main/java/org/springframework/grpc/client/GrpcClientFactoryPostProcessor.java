/*
 * Copyright 2024-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Post processor for {@link GrpcClientFactory} that applies the customizers and provides
 * a factory for client instances at runtime.
 *
 * @author Dave Syer
 */
public class GrpcClientFactoryPostProcessor implements ApplicationContextAware {

	private ApplicationContext context;

	private boolean initialized = false;

	private GrpcClientFactory registry;

	private void initialize(ApplicationContext context) {
		if (this.initialized || this.context == null) {
			return;
		}
		this.initialized = true;
		this.registry = new GrpcClientFactory(context);
		if (context.getBeanNamesForType(GrpcClientFactoryCustomizer.class).length > 0) {
			List<GrpcClientFactoryCustomizer> values = new ArrayList<>(
					context.getBeansOfType(GrpcClientFactoryCustomizer.class).values());
			AnnotationAwareOrderComparator.sort(values);
			values.forEach(customizer -> customizer.customize(this.registry));
		}
	}

	<T> T getClient(String target, Class<T> type, Class<?> factory) {
		initialize(this.context);
		return this.registry.getClient(target, (Class<T>) type, factory);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (applicationContext instanceof GenericApplicationContext generic) {
			this.context = generic;
		}
		else {
			throw new IllegalStateException("ApplicationContext must be a GenericApplicationContext");
		}
	}

}
