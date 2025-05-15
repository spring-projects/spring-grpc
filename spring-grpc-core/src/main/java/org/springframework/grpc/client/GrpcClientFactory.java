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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.grpc.internal.ClasspathScanner;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

/**
 * A factory of gRPC clients that can be used to create client stubs as beans in an
 * application context. The best way to interact with the factory is to declare a bean of
 * type {@link GrpcClientFactoryCustomizer} in the application context. The customizer
 * will be called with the factory before it is used to create the beans.
 *
 * @author Dave Syer
 */
public class GrpcClientFactory {

	private static final Set<Class<?>> DEFAULT_FACTORIES = new LinkedHashSet<>();

	private static final String FACTORIES_BEAN_DEFINITION_NAME = GrpcClientFactory.class.getName() + ".factories";

	private Map<Class<?>, StubFactory<?>> factories = new LinkedHashMap<>();

	private final ApplicationContext context;

	private Map<String, Supplier<ManagedChannel>> options = new HashMap<>();

	static {
		DEFAULT_FACTORIES.add((Class<? extends StubFactory<?>>) BlockingStubFactory.class);
		DEFAULT_FACTORIES.add((Class<? extends StubFactory<?>>) BlockingV2StubFactory.class);
		DEFAULT_FACTORIES.add((Class<? extends StubFactory<?>>) FutureStubFactory.class);
		DEFAULT_FACTORIES.add((Class<? extends StubFactory<?>>) ReactorStubFactory.class);
		DEFAULT_FACTORIES.add((Class<? extends StubFactory<?>>) SimpleStubFactory.class);
	}

	public GrpcClientFactory(ApplicationContext context) {
		this.context = context;
	}

	public <T> T getClient(String target, Class<T> type, Class<?> factory) {
		@SuppressWarnings("unchecked")
		StubFactory<T> stubs = (StubFactory<T>) findFactory(factory, type);
		Supplier<ManagedChannel> channel = this.options.get(target);
		if (channel == null) {
			channel = () -> channels().createChannel(target, ChannelBuilderOptions.defaults());
		}
		Supplier<ManagedChannel> finalChannel = channel;
		T client = (T) stubs.create(() -> finalChannel.get(), type);
		return client;
	}

	/**
	 * Register a channel factory for the given target. The channel will be created using
	 * the given options. If no options are provided, the default options will be used.
	 * @param target the name (or base url) of the target
	 * @param options the options to use to create the channel
	 */
	public void channel(String target, ChannelBuilderOptions options) {
		this.options.put(target, () -> channels().createChannel(target, options));
	}

	private StubFactory<?> findFactory(Class<?> factoryType, Class<?> type) {
		if (this.factories.isEmpty()) {
			List<StubFactory<?>> factories = new ArrayList<>();
			for (StubFactory<?> factory : this.context.getBeansOfType(StubFactory.class).values()) {
				factories.add(factory);
			}
			AnnotationAwareOrderComparator.sort(factories);
			for (StubFactory<?> factory : factories) {
				if (supports(factory.getClass(), type)) {
					this.factories.put(factory.getClass(), factory);
				}
			}
			for (Class<?> factory : DEFAULT_FACTORIES) {
				if (this.factories.containsKey(factory)) {
					continue;
				}
				this.factories.put(factory,
						(StubFactory<?>) this.context.getAutowireCapableBeanFactory().createBean(factory));
			}
		}
		StubFactory<?> factory = findFactory(this.factories, factoryType, type);
		if (factory == null) {
			throw new IllegalStateException(
					"Cannot find a suitable factory for " + type.getName() + " with factory " + factoryType);
		}
		return factory;
	}

	private static Class<?> findDefaultFactory(BeanDefinitionRegistry registry, Class<?> factoryType, Class<?> type) {
		if (factoryType != null && factoryType != UnspecifiedStubFactory.class) {
			return supports(factoryType, type) ? factoryType : null;
		}
		Set<Class<?>> factories = locateFactoryTypes(registry);
		for (Class<?> factory : factories) {
			if (supports(factory, type)) {
				return factory;
			}
		}
		return null;
	}

	private static Set<Class<?>> locateFactoryTypes(BeanDefinitionRegistry registry) {
		AbstractBeanDefinition beanDefinition;
		if (!registry.containsBeanDefinition(FACTORIES_BEAN_DEFINITION_NAME)) {
			// Stash the factories in a bean definition so we can find them later
			beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(StubFactoryProvider.class).getBeanDefinition();
			beanDefinition.setAttribute("factories", findStubFactoryTypes(registry));
			registry.registerBeanDefinition(FACTORIES_BEAN_DEFINITION_NAME, beanDefinition);
		}
		beanDefinition = (AbstractBeanDefinition) registry.getBeanDefinition(FACTORIES_BEAN_DEFINITION_NAME);
		@SuppressWarnings("unchecked")
		Set<Class<?>> factories = (Set<Class<?>>) beanDefinition.getAttribute("factories");
		return factories;

	}

	public static HashSet<Class<?>> findStubFactoryTypes(BeanDefinitionRegistry registry) {
		HashSet<Class<?>> factories = new HashSet<>();
		for (String name : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(name);
			Class<?> factory = resolveBeanClass(beanDefinition);
			if (factory != null && StubFactory.class.isAssignableFrom(factory)) {
				factories.add(factory);
			}
		}
		for (Class<?> factory : DEFAULT_FACTORIES) {
			if (!factories.contains(factory)) {
				factories.add(factory);
			}
		}
		return factories;
	}

	private static Class<?> resolveBeanClass(BeanDefinition beanDefinition) {
		if (beanDefinition instanceof AbstractBeanDefinition rootBeanDefinition) {
			if (rootBeanDefinition.hasBeanClass()) {
				return rootBeanDefinition.getBeanClass();
			}
		}
		return null;
	}

	private static boolean supports(Class<?> factory, Class<?> type) {
		// To avoid needing to instantiate the factory we use reflection to check for a
		// static supports() method. If it exists we call it.
		Method method = ReflectionUtils.findMethod(factory, "supports", Class.class);
		boolean supports = false;
		if (method != null) {
			ReflectionUtils.makeAccessible(method);
			try {
				supports = (boolean) ReflectionUtils.invokeMethod(method, null, type);
			}
			catch (Exception e) {
				// Ignore
			}
		}
		else {
			// If the factory is not one of the default factories, and doesn't have a
			// supports() method we assume it supports the supplied type
			supports = !DEFAULT_FACTORIES.contains(factory);
		}
		return supports;
	}

	private static StubFactory<?> findFactory(Map<Class<?>, StubFactory<?>> values, Class<?> factoryType,
			Class<?> type) {
		StubFactory<?> factory = null;
		if (factoryType != null && factoryType != UnspecifiedStubFactory.class) {
			factory = values.get(factoryType);
		}
		else {
			for (StubFactory<?> value : values.values()) {
				if (supports(value.getClass(), type)) {
					factory = value;
					break;
				}
			}
		}
		return factory;
	}

	private GrpcChannelFactory channels() {
		return this.context.getBean(GrpcChannelFactory.class);
	}

	public static void register(BeanDefinitionRegistry registry, GrpcClientRegistrationSpec spec) {
		spec = spec.prepare(registry);
		for (Class<?> type : spec.types()) {
			if (GrpcClientFactory.findDefaultFactory(registry, spec.factory(), type) == null) {
				continue;
			}
			RootBeanDefinition beanDef = (RootBeanDefinition) BeanDefinitionBuilder.rootBeanDefinition(type)
				.setLazyInit(true)
				.setFactoryMethodOnBean("getClient", GrpcClientFactoryPostProcessor.class.getName())
				.addConstructorArgValue(spec.target())
				.addConstructorArgValue(type)
				.addConstructorArgValue(spec.factory())
				.getBeanDefinition();
			beanDef.setTargetType(type);
			String beanName = StringUtils.hasText(spec.prefix()) ? spec.prefix() + type.getSimpleName()
					: StringUtils.uncapitalize(type.getSimpleName());
			if (!registry.containsBeanDefinition(beanName)) {
				// Avoid registering the same bean definition multiple times
				registry.registerBeanDefinition(beanName, beanDef);
			}
		}
	}

	public record GrpcClientRegistrationSpec(String prefix, Class<? extends StubFactory<?>> factory, String target,
			Class<?>[] types, String[] packages) {

		private static ClasspathScanner SCANNER = new ClasspathScanner();

		public static GrpcClientRegistrationSpec defaults() {
			return new GrpcClientRegistrationSpec("default", new Class[0]);
		}

		private GrpcClientRegistrationSpec prepare(BeanDefinitionRegistry registry) {
			Set<Class<?>> allTypes = new HashSet<>();
			allTypes.addAll(Set.of(this.types));
			for (String basePackage : this.packages) {
				TypeFilter filter = new TypeFilter() {
					@Override
					public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
							throws IOException {
						Class<?> type = ClassUtils.resolveClassName(metadataReader.getClassMetadata().getClassName(),
								ClasspathScanner.class.getClassLoader());
						return supports(GrpcClientRegistrationSpec.this.factory, type);
					}
				};
				for (Class<?> type : SCANNER.scan(basePackage, filter)) {
					if (findDefaultFactory(registry, this.factory, type) != null) {
						allTypes.add(type);
					}
				}
			}
			@SuppressWarnings("unchecked")
			Class<? extends AbstractStub<?>>[] newTypes = allTypes.toArray(new Class[0]);
			return new GrpcClientRegistrationSpec(this.prefix, this.factory, this.target, newTypes, new String[0]);
		}

		public static GrpcClientRegistrationSpec of(String target) {
			return new GrpcClientRegistrationSpec(target, new Class[0]);
		}

		public GrpcClientRegistrationSpec(String target, Class<?>[] types) {
			this("", UnspecifiedStubFactory.class, target, types, new String[0]);
		}

		public GrpcClientRegistrationSpec(String prefix, String target, Class<?>[] types) {
			this(prefix, UnspecifiedStubFactory.class, target, types, new String[0]);
		}

		public GrpcClientRegistrationSpec factory(Class<? extends StubFactory<?>> factory) {
			return new GrpcClientRegistrationSpec(this.prefix, factory, this.target, this.types, this.packages);
		}

		public GrpcClientRegistrationSpec types(Class<?>... types) {
			return new GrpcClientRegistrationSpec(this.prefix, this.factory, this.target, types, this.packages);
		}

		public GrpcClientRegistrationSpec prefix(String prefix) {
			if (StringUtils.hasText(prefix)) {
				return new GrpcClientRegistrationSpec(prefix, this.factory, this.target, this.types, this.packages);
			}
			else {
				return new GrpcClientRegistrationSpec("", this.factory, this.target, this.types, this.packages);
			}
		}

		public GrpcClientRegistrationSpec packages(String... packages) {
			Set<String> allPackages = new HashSet<>();
			for (String pkg : packages) {
				if (StringUtils.hasText(pkg)) {
					allPackages.add(pkg);
				}
			}
			for (String pkg : this.packages) {
				if (StringUtils.hasText(pkg)) {
					allPackages.add(pkg);
				}
			}
			return new GrpcClientRegistrationSpec(this.prefix, this.factory, this.target, this.types,
					allPackages.toArray(new String[0]));
		}

		public GrpcClientRegistrationSpec packageClasses(Class<?>... packageClasses) {
			String[] packages = new String[packageClasses.length];
			for (Class<?> basePackageClass : packageClasses) {
				for (int i = 0; i < packageClasses.length; i++) {
					String basePackage = ClassUtils.getPackageName(basePackageClass);
					packages[i] = basePackage;
				}
			}
			return packages(packages);
		}
	}

	static class StubFactoryProvider {

	}

}
