/*
 * Copyright 2023-present the original author or authors.
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
package org.springframework.grpc.autoconfigure.server.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;

/**
 * Tests for {@link ActuatorHealthAdapter}.
 */
class ActuatorHealthAdapterTests {

	private HealthStatusManager mockHealthStatusManager;

	private HealthEndpoint mockHealthEndpoint;

	private StatusAggregator mockStatusAggregator;

	@BeforeEach
	void prepareMocks() {
		mockHealthStatusManager = Mockito.mock();
		mockHealthEndpoint = Mockito.mock();
		mockStatusAggregator = Mockito.mock();
	}

	@Test
	void whenIndicatorPathsFoundStatusIsUpdated() {
		var service1 = "check1";
		var service2 = "component2/check2";
		var service3 = "component3a/component3b/check3";
		when(mockHealthEndpoint.healthForPath("check1")).thenReturn(Health.up().build());
		when(mockHealthEndpoint.healthForPath("component2", "check2")).thenReturn(Health.down().build());
		when(mockHealthEndpoint.healthForPath("component3a", "component3b", "check3"))
			.thenReturn(Health.unknown().build());
		when(mockStatusAggregator.getAggregateStatus(anySet())).thenReturn(Status.UNKNOWN);
		var healthAdapter = new ActuatorHealthAdapter(mockHealthStatusManager, mockHealthEndpoint, mockStatusAggregator,
				true, List.of(service1, service2, service3));
		healthAdapter.updateHealthStatus();
		verify(mockHealthStatusManager).setStatus(service1, ServingStatus.SERVING);
		verify(mockHealthStatusManager).setStatus(service2, ServingStatus.NOT_SERVING);
		verify(mockHealthStatusManager).setStatus(service3, ServingStatus.UNKNOWN);
		ArgumentCaptor<Set<Status>> statusesArgCaptor = ArgumentCaptor.captor();
		verify(mockStatusAggregator).getAggregateStatus(statusesArgCaptor.capture());
		assertThat(statusesArgCaptor.getValue())
			.containsExactlyInAnyOrderElementsOf(Set.of(Status.UP, Status.DOWN, Status.UNKNOWN));
		verify(mockHealthStatusManager).setStatus("", ServingStatus.UNKNOWN);
	}

	@Test
	void whenOverallHealthIsFalseOverallStatusIsNotUpdated() {
		var service1 = "check1";
		when(mockHealthEndpoint.healthForPath("check1")).thenReturn(Health.up().build());
		var healthAdapter = new ActuatorHealthAdapter(mockHealthStatusManager, mockHealthEndpoint, mockStatusAggregator,
				false, List.of(service1));
		healthAdapter.updateHealthStatus();
		verifyNoInteractions(mockStatusAggregator);
		verify(mockHealthStatusManager, never()).setStatus(eq(""), any(ServingStatus.class));
	}

	@Test
	void whenIndicatorPathNotFoundStatusIsNotUpdated() {
		var healthAdapter = new ActuatorHealthAdapter(mockHealthStatusManager, mockHealthEndpoint, mockStatusAggregator,
				false, List.of("check1"));
		healthAdapter.updateHealthStatus();
		verifyNoInteractions(mockHealthStatusManager);
	}

	@Test
	void whenNoIndicatorPathsSpecifiedThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ActuatorHealthAdapter(mockHealthStatusManager, mockHealthEndpoint,
					mockStatusAggregator, false, List.of()))
			.withMessage("at least one health indicator path is required");
	}

	@Nested
	class ToServingStatusApi {

		private final ActuatorHealthAdapter healthAdapter = new ActuatorHealthAdapter(mockHealthStatusManager,
				mockHealthEndpoint, mockStatusAggregator, false, List.of("check1"));

		@Test
		void whenActuatorStatusIsUpThenServingStatusIsUp() {
			assertThat(this.healthAdapter.toServingStatus(Status.UP.getCode())).isEqualTo(ServingStatus.SERVING);
		}

		@Test
		void whenActuatorStatusIsUnknownThenServingStatusIsUnknown() {
			assertThat(this.healthAdapter.toServingStatus(Status.UNKNOWN.getCode())).isEqualTo(ServingStatus.UNKNOWN);
		}

		@Test
		void whenActuatorStatusIsDownThenServingStatusIsNotServing() {
			assertThat(this.healthAdapter.toServingStatus(Status.DOWN.getCode())).isEqualTo(ServingStatus.NOT_SERVING);
		}

		@Test
		void whenActuatorStatusIsOutOfServiceThenServingStatusIsNotServing() {
			assertThat(this.healthAdapter.toServingStatus(Status.OUT_OF_SERVICE.getCode()))
				.isEqualTo(ServingStatus.NOT_SERVING);
		}

	}

}
