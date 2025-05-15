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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchIllegalStateException;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;
import org.springframework.grpc.client.GrpcClientFactoryTests.MyProto.MyStub;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

public class GrpcClientFactoryTests {

	private GrpcChannelFactory channelFactory = Mockito.mock(GrpcChannelFactory.class);

	private StaticApplicationContext context = new StaticApplicationContext();

	private GrpcClientFactory factory;

	GrpcClientFactoryTests() {
		Mockito.when(channelFactory.createChannel(Mockito.anyString(), Mockito.any()))
			.thenReturn(Mockito.mock(ManagedChannel.class));
		context.registerBean(GrpcChannelFactory.class, () -> channelFactory);
		factory = new GrpcClientFactory(context);
	}

	@Test
	void testRegisterAndCreate() {
		GrpcClientFactory.register(context, new GrpcClientRegistrationSpec("local", new Class[] { MyStub.class }));
		assertThat(factory.getClient("local", MyStub.class, null)).isNotNull();
	}

	@Test
	void testNoStubFactory() {
		GrpcClientFactory.register(context, new GrpcClientRegistrationSpec("local", new Class[] { OtherStub.class }));
		catchIllegalStateException(() -> factory.getClient("local", OtherStub.class, null));
	}

	@Test
	void testCustomStubFactory() {
		context.registerBean(OtherStubFactory.class, () -> new OtherStubFactory());
		GrpcClientFactory.register(context, new GrpcClientRegistrationSpec("local", new Class[] { OtherStub.class }));
		assertThat(factory.getClient("local", OtherStub.class, OtherStubFactory.class)).isNotNull();
	}

	@Test
	void testWithExplicitStubFactory() {
		context.registerBean(OtherStubFactory.class, () -> new OtherStubFactory());
		GrpcClientFactory.register(context, new GrpcClientRegistrationSpec("local", new Class[] { OtherStub.class })
			.factory(OtherStubFactory.class));
		assertThat(factory.getClient("local", OtherStub.class, null)).isNotNull();
	}

	@Test
	void testAnnotationConfig() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(MyConfiguration.class);
		context.registerBean(GrpcChannelFactory.class, () -> channelFactory);
		GrpcClientFactory.register(context, new GrpcClientRegistrationSpec("local", new Class[] { OtherStub.class }));
		context.refresh();
		factory = new GrpcClientFactory(context);
		assertThat(factory.getClient("local", OtherStub.class, null)).isNotNull();
	}

	static class OtherStubFactory implements StubFactory<OtherStub> {

		@Override
		public OtherStub create(Supplier<ManagedChannel> channel, Class<? extends OtherStub> type) {
			return new OtherStub(channel.get());
		}

		static boolean supports(Class<?> type) {
			return OtherStub.class.isAssignableFrom(type);
		}

	}

	static class OtherStub extends AbstractStub<OtherStub> {

		OtherStub(Channel channel) {
			super(channel);
		}

		@Override
		protected OtherStub build(Channel channel, CallOptions callOptions) {
			return new OtherStub(channel);
		}

	}

	static class MyProto {

		public static MyStub newStub(Channel channel) {
			return new MyStub(channel);
		}

		static class MyStub extends AbstractStub<MyStub> {

			MyStub(Channel channel) {
				super(channel);
			}

			@Override
			protected MyStub build(Channel channel, CallOptions callOptions) {
				return new MyStub(channel);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfiguration {

		@Bean
		OtherStubFactory otherStubFactory() {
			return new OtherStubFactory();
		}

	}

}
