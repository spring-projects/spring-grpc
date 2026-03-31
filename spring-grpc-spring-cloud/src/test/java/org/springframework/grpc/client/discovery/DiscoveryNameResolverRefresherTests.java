/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.grpc.client.discovery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;

/**
 * Tests for {@link DiscoveryNameResolverRefresher}.
 */
class DiscoveryNameResolverRefresherTests {

	@Test
	void heartbeatRefreshesAllResolvers() {
		DiscoveryClientNameResolverProvider provider = mock(DiscoveryClientNameResolverProvider.class);
		var refresher = new DiscoveryNameResolverRefresher(provider);
		refresher.onHeartbeat(new HeartbeatEvent(this, "test"));
		verify(provider).refreshAll("heartbeat-event");
	}

}
