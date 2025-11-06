package org.springframework.grpc.sample;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import io.grpc.Metadata;
import io.grpc.ServerInterceptor;

@SpringBootApplication
@EnableGlobalAuthentication
class GrpcServerApplication {

	public static final Metadata.Key<String> USER_KEY = Metadata.Key.of("X-USER", Metadata.ASCII_STRING_MARSHALLER);

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

	@Bean
	InMemoryUserDetailsManager inMemoryUserDetailsManager() {
		return new InMemoryUserDetailsManager(
				User.withUsername("user").password("{noop}user").authorities("ROLE_USER").build(),
				User.withUsername("admin").password("{noop}admin").authorities("ROLE_ADMIN").build());
	}

	@Bean
	@GlobalServerInterceptor
	ServerInterceptor securityInterceptor(GrpcSecurity security) throws Exception {
		return security
			.authorizeRequests(requests -> requests.methods("Simple/StreamHello")
				.hasAuthority("ROLE_ADMIN")
				.methods("Simple/SayHello")
				.hasAuthority("ROLE_USER")
				.methods("grpc.*/*")
				.permitAll()
				.allRequests()
				.denyAll())
			.httpBasic(withDefaults())
			.preauth(withDefaults())
			.build();

	}

}
