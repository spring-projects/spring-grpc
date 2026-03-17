package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.experimental.boot.server.exec.CommonsExecWebServerFactoryBean;
import org.springframework.experimental.boot.server.exec.MavenClasspathEntry;
import org.springframework.experimental.boot.test.context.EnableDynamicProperty;
import org.springframework.experimental.boot.test.context.OAuth2ClientProviderIssuerUri;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.interceptor.security.BearerTokenAuthenticationInterceptor;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "debug=true", "spring.grpc.server.port=0",
				"spring.grpc.client.channel.default.target=0.0.0.0:${local.grpc.server.port}",
				"spring.grpc.client.channel.stub.target=0.0.0.0:${local.grpc.server.port}",
				"spring.grpc.client.channel.secure.target=0.0.0.0:${local.grpc.server.port}" })
@DirtiesContext
public class GrpcServerApplicationTests {

	public static void main(String[] args) {
		SpringApplication.from(GrpcServerApplication::main).with(ExtraConfiguration.class).run(args);
	}

	@Autowired
	@Qualifier("simpleBlockingStub")
	private SimpleGrpc.SimpleBlockingStub stub;

	@Autowired
	@Qualifier("secureSimpleBlockingStub")
	private SimpleGrpc.SimpleBlockingStub basic;

	@Test
	void contextLoads() {
	}

	@Test
	void unauthenticated() {
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> stub.sayHello(HelloRequest.newBuilder().setName("Alien").build()));
		assertEquals(Code.UNAUTHENTICATED, exception.getStatus().getCode());
	}

	@Test
	void authenticated() {
		// The token has no scopes but none are required
		HelloReply response = basic.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@TestConfiguration(proxyBeanMethods = false)
	@EnableDynamicProperty
	@ImportGrpcClients(target = "stub", types = { SimpleGrpc.SimpleBlockingStub.class })
	@ImportGrpcClients(target = "secure", prefix = "secure", types = { SimpleGrpc.SimpleBlockingStub.class })
	static class ExtraConfiguration {

		private String token;

		@Bean
		@OAuth2ClientProviderIssuerUri
		static CommonsExecWebServerFactoryBean authServer() {
			return CommonsExecWebServerFactoryBean.builder()
				.useGenericSpringBootMain()
				.classpath(classpath -> classpath
					.entries(MavenClasspathEntry.springBootStarter("oauth2-authorization-server")));
		}

		@Bean
		GrpcChannelBuilderCustomizer<?> stubs(ObjectProvider<ReactiveClientRegistrationRepository> context) {
			return GrpcChannelBuilderCustomizer.matching("secure",
					builder -> builder.intercept(new BearerTokenAuthenticationInterceptor(() -> token(context))));
		}

		private String token(ObjectProvider<ReactiveClientRegistrationRepository> context) {
			if (this.token == null) { // ... plus we could check for expiry
				RestClientClientCredentialsTokenResponseClient creds = new RestClientClientCredentialsTokenResponseClient();
				ReactiveClientRegistrationRepository registry = context.getObject();
				ClientRegistration reg = registry.findByRegistrationId("spring").block();
				this.token = creds.getTokenResponse(new OAuth2ClientCredentialsGrantRequest(reg))
					.getAccessToken()
					.getTokenValue();
			}
			return this.token;
		}

	}

}
