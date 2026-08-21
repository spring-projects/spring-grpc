package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.experimental.boot.server.exec.CommonsExecWebServerFactoryBean;
import org.springframework.experimental.boot.server.exec.MavenClasspathEntry;
import org.springframework.experimental.boot.test.context.EnableDynamicProperty;
import org.springframework.experimental.boot.test.context.OAuth2ClientProviderIssuerUri;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.interceptor.security.BearerTokenAuthenticationInterceptor;
import org.springframework.grpc.client.interceptor.security.ClientCredentialsTokenSupplier;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

/**
 * Verifies that {@link GrpcSecurity} can authenticate opaque tokens by introspection,
 * rather than the JWTs used by the main sample application.
 */
@SpringBootTest(properties = { "spring.grpc.server.port=0",
		"spring.grpc.client.channel.default.target=0.0.0.0:${local.grpc.server.port}",
		"spring.grpc.client.channel.stub.target=0.0.0.0:${local.grpc.server.port}",
		"spring.grpc.client.channel.secure.target=0.0.0.0:${local.grpc.server.port}",
		"spring.main.allow-bean-definition-overriding=true" })
@DirtiesContext
public class OpaqueTokenServerApplicationTests {

	@Autowired
	@Qualifier("simpleBlockingStub")
	private SimpleGrpc.SimpleBlockingStub stub;

	@Autowired
	@Qualifier("secureSimpleBlockingStub")
	private SimpleGrpc.SimpleBlockingStub secure;

	@Test
	void unauthenticated() {
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> this.stub.sayHello(HelloRequest.newBuilder().setName("Alien").build()));
		assertEquals(Code.UNAUTHENTICATED, exception.getStatus().getCode());
	}

	@Test
	void authenticatedByIntrospection() {
		HelloReply response = this.secure.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@Test
	void unauthorizedWhenScopeMissing() {
		// The token has no scopes and scope=profile is required
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> this.secure.streamHello(HelloRequest.newBuilder().setName("Alien").build()).next());
		assertEquals(Code.PERMISSION_DENIED, exception.getStatus().getCode());
	}

	@TestConfiguration(proxyBeanMethods = false)
	@EnableDynamicProperty
	@ImportGrpcClients(target = "stub", types = { SimpleGrpc.SimpleBlockingStub.class })
	@ImportGrpcClients(target = "secure", prefix = "secure", types = { SimpleGrpc.SimpleBlockingStub.class })
	static class ExtraConfiguration {

		@Bean
		@OAuth2ClientProviderIssuerUri
		static CommonsExecWebServerFactoryBean authServer() {
			return CommonsExecWebServerFactoryBean.builder()
				.useGenericSpringBootMain()
				.classpath(classpath -> classpath
					.entries(MavenClasspathEntry.springBootStarter("oauth2-authorization-server")));
		}

		@Bean
		@GlobalServerInterceptor
		AuthenticationProcessInterceptor jwtSecurityFilterChain(GrpcSecurity grpc,
				@Value("${spring.security.oauth2.client.provider.spring.issuer-uri}") String issuerUri)
				throws Exception {
			return grpc
				.authorizeRequests(requests -> requests.methods("Simple/StreamHello")
					.hasAuthority("SCOPE_profile")
					.methods("Simple/SayHello")
					.authenticated()
					.methods("grpc.*/*")
					.permitAll()
					.allRequests()
					.denyAll())
				.oauth2ResourceServer(resourceServer -> resourceServer
					.opaqueToken(opaqueToken -> opaqueToken.introspectionUri(issuerUri + "/oauth2/introspect")
						.introspectionClientCredentials("spring", "secret")))
				.build();
		}

		@Bean
		GrpcChannelBuilderCustomizer<?> stubs(ObjectProvider<ClientRegistrationRepository> context) {
			return GrpcChannelBuilderCustomizer.matching("secure",
					builder -> builder.intercept(new BearerTokenAuthenticationInterceptor(
							new ClientCredentialsTokenSupplier(context.getObject(), () -> "spring"))));
		}

	}

}
