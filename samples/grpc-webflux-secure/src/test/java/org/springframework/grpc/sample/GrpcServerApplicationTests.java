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
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.grpc.client.default-channel.address=0.0.0.0:${local.grpc.port}",
				"spring.grpc.server.port=0" })
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
				.classpath(classpath -> classpath.entries(new MavenClasspathEntry(
						"org.springframework.boot:spring-boot-starter-oauth2-authorization-server:3.5.5")));
		}

		@Bean
		GrpcChannelBuilderCustomizer<?> stubs(ObjectProvider<ClientRegistrationRepository> context) {
			return GrpcChannelBuilderCustomizer.matching("secure",
					builder -> builder.intercept(new BearerTokenAuthenticationInterceptor(() -> token(context))));
		}

		private String token(ObjectProvider<ClientRegistrationRepository> context) {
			if (this.token == null) { // ... plus we could check for expiry
				RestClientClientCredentialsTokenResponseClient creds = new RestClientClientCredentialsTokenResponseClient();
				ClientRegistrationRepository registry = context.getObject();
				ClientRegistration reg = registry.findByRegistrationId("spring");
				this.token = creds.getTokenResponse(new OAuth2ClientCredentialsGrantRequest(reg))
					.getAccessToken()
					.getTokenValue();
			}
			return this.token;
		}

	}

}
