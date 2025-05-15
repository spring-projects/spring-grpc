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
package org.springframework.grpc.client.aot;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.MethodParameter;
import org.springframework.grpc.client.GrpcClientFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.google.protobuf.AbstractMessage;
import io.grpc.stub.AbstractStub;

public class ClientBeanRegistrationsAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	@Nullable
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		Set<Type> registrations = new HashSet<>();
		Set<Class<?>> resources = new HashSet<>();

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			RegisteredBean registeredBean = RegisteredBean.of(beanFactory, beanName);
			if (AbstractStub.class.isAssignableFrom(registeredBean.getBeanClass())) {
				Class<?> type = registeredBean.getBeanClass().getEnclosingClass();
				if (type != null) {
					registrations.add(type);
					resources.add(registeredBean.getBeanClass());
				}
				registrations.addAll(findMessageTypes(registeredBean.getBeanClass()));
			}
		}

		if (registrations.isEmpty()) {
			return null;
		}

		Set<Type> factories = new HashSet<>();
		if (beanFactory instanceof DefaultListableBeanFactory listable) {
			HashSet<Class<?>> types = GrpcClientFactory.findStubFactoryTypes(listable);
			factories.addAll(types);
		}

		return new ClientBeanRegistrationsAotContribution(registrations, factories, resources);
	}

	private Collection<Type> findMessageTypes(Class<?> beanClass) {
		Set<Type> types = new HashSet<>();
		ReflectionUtils.doWithMethods(beanClass, method -> {
			MethodParameter param = MethodParameter.forExecutable(method, -1);
			findMessageType(param, types);
			for (Parameter type : method.getParameters()) {
				param = MethodParameter.forParameter(type);
				findMessageType(param, types);
			}
		}, method -> Modifier.isPublic(method.getModifiers()) && ReflectionUtils.findMethod(AbstractStub.class,
				method.getName(), method.getParameterTypes()) == null);
		return types;
	}

	private void findMessageType(MethodParameter param, Set<Type> types) {
		Class<?> type = param.getParameterType();
		if (AbstractMessage.class.isAssignableFrom(type)) {
			addMessageType(types, type);
		}
		else {
			Type generic = param.getGenericParameterType();
			if (generic instanceof Class cls) {
				if (AbstractMessage.class.isAssignableFrom(cls)) {
					addMessageType(types, generic);
				}
			}
		}
	}

	private void addMessageType(Set<Type> types, Type type) {
		types.add(type);
		types.add(ClassUtils.resolveClassName(type.getTypeName() + ".Builder", null));
	}

	static class ClientBeanRegistrationsAotContribution implements BeanFactoryInitializationAotContribution {

		private Set<Type> types;

		private Set<Class<?>> resources;

		private Set<Type> factories;

		ClientBeanRegistrationsAotContribution(Set<Type> types, Set<Type> factories, Set<Class<?>> resources) {
			this.types = types;
			this.factories = factories;
			this.resources = resources;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			ReflectionHints hints = generationContext.getRuntimeHints().reflection();
			// Registers all stubs and message types for reflection. Not all the message
			// types will be used but it is simpler to register them all.
			for (Type type : this.types) {
				hints.registerType(TypeReference.of(type.getTypeName()), MemberCategory.INVOKE_PUBLIC_METHODS);
			}
			for (Type type : this.factories) {
				hints.registerType(TypeReference.of(type.getTypeName()), MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			}
			ResourceHints resources = generationContext.getRuntimeHints().resources();
			// We only really need this if we are scanning. Some stubs are not scanned
			// anyway, and scanning should be unnecessary for AOT, but this works and can
			// be refined later.
			for (Class<?> resource : this.resources) {
				resources.registerType(resource);
			}
		}

	}

}
