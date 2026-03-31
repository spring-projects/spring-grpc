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

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.log.LogAccessor;

/**
 * Refreshes discovery-backed resolvers when the Spring Cloud registry changes.
 *
 * @author Hyacinth Contributor
 */
public class DiscoveryNameResolverRefresher {

	private final LogAccessor logger = new LogAccessor(getClass());

	private final DiscoveryClientNameResolverProvider provider;

	public DiscoveryNameResolverRefresher(DiscoveryClientNameResolverProvider provider) {
		this.provider = provider;
	}

	@EventListener(HeartbeatEvent.class)
	public void onHeartbeat(HeartbeatEvent event) {
		this.logger.debug(() -> "Received HeartbeatEvent, refreshing discovery resolvers. value=" + event.getValue());
		this.provider.refreshAll("heartbeat-event");
	}

}
