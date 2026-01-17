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

package org.springframework.boot.grpc.server.autoconfigure.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnGrpcServerEnabled;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnSpringGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcAdviceDiscoverer;
import org.springframework.grpc.server.advice.GrpcAdviceExceptionHandler;
import org.springframework.grpc.server.advice.GrpcExceptionHandlerMethodResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link GrpcAdvice @GrpcAdvice}
 * exception handling.
 *
 * @author Oleksandr Shevchenko
 * @since 1.1.0
 */
@AutoConfiguration(before = GrpcExceptionHandlerAutoConfiguration.class)
@ConditionalOnSpringGrpc
@ConditionalOnGrpcServerEnabled("exception-handler")
@ConditionalOnBean(annotation = GrpcAdvice.class)
public class GrpcAdviceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	GrpcAdviceDiscoverer grpcAdviceDiscoverer() {
		return new GrpcAdviceDiscoverer();
	}

	@Bean
	@ConditionalOnMissingBean
	GrpcExceptionHandlerMethodResolver grpcExceptionHandlerMethodResolver(GrpcAdviceDiscoverer grpcAdviceDiscoverer) {
		return new GrpcExceptionHandlerMethodResolver(grpcAdviceDiscoverer);
	}

	@Bean
	GrpcAdviceExceptionHandler grpcAdviceExceptionHandler(
			GrpcExceptionHandlerMethodResolver grpcExceptionHandlerMethodResolver) {
		return new GrpcAdviceExceptionHandler(grpcExceptionHandlerMethodResolver);
	}

}
