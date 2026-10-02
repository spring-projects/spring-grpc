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

package org.springframework.grpc.transcoding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * Configuration for gRPC-JSON transcoding. Applications opt in to transcoding by
 * importing this configuration.
 *
 * @author Aleksander Brzozowski
 */
@Configuration(proxyBeanMethods = false)
public class GrpcTranscodingConfiguration {

	@Bean(destroyMethod = "")
	Channel grpcTranscodingChannel(Environment environment, GrpcChannelFactory channelFactory) {
		String inProcessName = environment.getProperty("spring.grpc.transcoding.in-process-name", "grpc-transcoding");
		ChannelBuilderOptions options = ChannelBuilderOptions.defaults()
			.withInterceptorsMerge(true)
			.<InProcessChannelBuilder>withCustomizer((__, builder) -> builder.directExecutor());
		return channelFactory.createChannel("in-process:" + inProcessName, options);
	}

	@Bean
	ProtobufJsonFormatHttpMessageConverter protobufJsonFormatHttpMessageConverter() {
		return new ProtobufJsonFormatHttpMessageConverter();
	}

}
