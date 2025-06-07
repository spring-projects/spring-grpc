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
package org.springframework.grpc.client;

import java.util.List;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * {@link GrpcChannelFactory} that creates in-process gRPC channels.
 *
 * @author Chris Bono
 */
public class InProcessGrpcChannelFactory extends DefaultGrpcChannelFactory<InProcessChannelBuilder> {

	/**
	 * Construct an in-process channel factory instance and sets the
	 * {@link #setVirtualTargets virtualTargets} to the identity function so that the
	 * exact passed in target string is used as the target of the channel factory.
	 * @param globalCustomizers the global customizers to apply to all created channels
	 * @param interceptorsConfigurer configures the client interceptors on the created
	 * channels
	 */
	public InProcessGrpcChannelFactory(List<GrpcChannelBuilderCustomizer<InProcessChannelBuilder>> globalCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		super(globalCustomizers, interceptorsConfigurer);
		setVirtualTargets((p) -> p);
	}

	/**
	 * Whether this factory supports the given target string. The target can be either a
	 * valid nameresolver-compliant URI, an authority string as described in
	 * {@link Grpc#newChannelBuilder(String, ChannelCredentials)}.
	 * @param target the target string as described in method javadocs
	 * @return true if the target begins with 'in-process:'
	 */
	@Override
	public boolean supports(String target) {
		return target.startsWith("in-process:");
	}

	@Override
	protected InProcessChannelBuilder newChannelBuilder(String target, ChannelCredentials creds) {
		return InProcessChannelBuilder.forName(target.substring(11));
	}

}
