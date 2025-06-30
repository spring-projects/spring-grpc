/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.grpc.autoconfigure.server;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.util.unit.DataSize;

import io.grpc.BindableService;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side components.
 * <p>
 * gRPC must be on the classpath and at least one {@link BindableService} bean registered
 * in the context in order for the auto-configuration to execute.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Toshiaki Maki
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnGrpcServerEnabled
@ConditionalOnBean(BindableService.class)
public class GrpcServerFactoryAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnNativeGrpcServerCondition.class)
	static class GrpcServerFactoryConfiguration {

		@Configuration(proxyBeanMethods = false)
		@Import({ GrpcServerFactoryConfigurations.ShadedNettyServerFactoryConfiguration.class,
				GrpcServerFactoryConfigurations.NettyServerFactoryConfiguration.class,
				GrpcServerFactoryConfigurations.InProcessServerFactoryConfiguration.class })
		static class NettyServerFactoryConfiguration {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnGrpcServletCondition.class)
	public static class GrpcServletConfiguration {

		private static Log logger = LogFactory.getLog(GrpcServletConfiguration.class);

		@Bean
		public ServletRegistrationBean<GrpcServlet> grpcServlet(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers) {
			List<String> serviceNames = serviceDiscoverer.listServiceNames();
			if (logger.isInfoEnabled()) {
				serviceNames.forEach(service -> logger.info("Registering gRPC service: " + service));
			}
			List<String> paths = serviceNames.stream()
				.map(service -> "/" + service + "/*")
				.collect(Collectors.toList());
			ServletServerBuilder servletServerBuilder = new ServletServerBuilder();
			serviceDiscoverer.findServices()
				.stream()
				.map(serviceConfigurer::configure)
				.forEach(servletServerBuilder::addService);
			PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
			mapper.from(properties.getMaxInboundMessageSize())
				.asInt(DataSize::toBytes)
				.to(servletServerBuilder::maxInboundMessageSize);
			serverBuilderCustomizers.customize(servletServerBuilder);
			ServletRegistrationBean<GrpcServlet> servlet = new ServletRegistrationBean<>(
					servletServerBuilder.buildServlet());
			servlet.setUrlMappings(paths);
			return servlet;
		}

		@Configuration(proxyBeanMethods = false)
		@Import(GrpcServerFactoryConfigurations.InProcessServerFactoryConfiguration.class)
		static class InProcessConfiguration {

		}

	}

	public static class OnGrpcServletCondition extends AllNestedConditions {

		OnGrpcServletCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		static class OnServletWebApplication {

		}

		@ConditionalOnClass(GrpcServlet.class)
		static class OnGrpcServletClass {

		}

		@ConditionalOnProperty(prefix = "spring.grpc.server", name = "servlet.enabled", havingValue = "true",
				matchIfMissing = true)
		static class OnExplicitlyEnabled {

		}

	}

	public static class OnNativeGrpcServerCondition extends AnyNestedCondition {

		OnNativeGrpcServerCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnNotWebApplication
		static class OnNonWebApplication {

		}

		@ConditionalOnMissingClass("io.grpc.servlet.jakarta.GrpcServlet")
		static class OnGrpcServletClass {

		}

		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		@ConditionalOnClass(GrpcServlet.class)
		@ConditionalOnProperty(prefix = "spring.grpc.server", name = "servlet.enabled", havingValue = "false",
				matchIfMissing = false)
		static class OnExplicitlyDisabledServlet {

		}

		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
		static class OnExplicitlyDisabledWebflux {

		}

	}

}
