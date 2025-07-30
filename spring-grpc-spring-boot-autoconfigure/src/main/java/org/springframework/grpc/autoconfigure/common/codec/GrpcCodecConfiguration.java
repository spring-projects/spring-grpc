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

package org.springframework.grpc.autoconfigure.common.codec;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.Codec;
import io.grpc.Compressor;
import io.grpc.CompressorRegistry;
import io.grpc.Decompressor;
import io.grpc.DecompressorRegistry;

/**
 * The configuration that contains all codec related beans for clients/servers.
 *
 * @author Andrei Lisa
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Codec.class)
public class GrpcCodecConfiguration {

	@Bean
	CompressorRegistry compressorRegistry(ObjectProvider<Compressor> compressors) {
		CompressorRegistry registry = CompressorRegistry.getDefaultInstance();
		compressors.orderedStream().forEachOrdered(registry::register);
		return registry;
	}

	@Bean
	DecompressorRegistry decompressorRegistry(ObjectProvider<Decompressor> decompressors) {
		DecompressorRegistry registry = DecompressorRegistry.getDefaultInstance();
		decompressors.orderedStream().forEachOrdered(decompressor -> registry.with(decompressor, false));
		return registry;
	}

}
