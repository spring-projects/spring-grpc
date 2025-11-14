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

package org.springframework.grpc.client;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import org.springframework.grpc.client.DefaultGrpcChannelFactory.ManagedChannelWithShutdown;
import org.springframework.test.util.ReflectionTestUtils;

import io.grpc.ManagedChannel;

/**
 * Tests for shutdown process in {@link DefaultGrpcChannelFactory}.
 */
class DefaultGrpcChannelFactoryShutdownTests {

	@Test
	void channelsAreGracefullyShutdown() throws InterruptedException {
		this.channelsShutdownAsExpected(false);
	}

	@Test
	void whenChannelExceedsAwaitTimeOtherChannelsAreStillShutdownGracefully() throws InterruptedException {
		this.channelsShutdownAsExpected(true);
	}

	private void channelsShutdownAsExpected(boolean exceedAwaitTime) throws InterruptedException {
		var channelFactory = new DefaultGrpcChannelFactory<>(List.of(), mock());
		channelFactory.setVirtualTargets(path -> path);

		// create channels using factory and options
		var c1 = channelFactory.createChannel("c1",
				ChannelBuilderOptions.defaults().withShutdownGracePeriod(Duration.ofSeconds(7)));
		var c2 = channelFactory.createChannel("c2",
				ChannelBuilderOptions.defaults().withShutdownGracePeriod(Duration.ofSeconds(5)));
		var c3 = channelFactory.createChannel("c3",
				ChannelBuilderOptions.defaults().withShutdownGracePeriod(Duration.ofSeconds(10)));

		// spy each channel to wait accordingly
		var c1Spy = setupSpy(c1);
		var c2Spy = setupSpy(c2, exceedAwaitTime);
		var c3Spy = setupSpy(c3);

		// replace factory channels with spy channels
		List<ManagedChannelWithShutdown> spiedChannels = new ArrayList<>();
		spiedChannels.add(new ManagedChannelWithShutdown(c1Spy, Duration.ofSeconds(7)));
		spiedChannels.add(new ManagedChannelWithShutdown(c2Spy, Duration.ofSeconds(5)));
		spiedChannels.add(new ManagedChannelWithShutdown(c3Spy, Duration.ofSeconds(10)));
		ReflectionTestUtils.setField(channelFactory, "channels", spiedChannels);

		// invoke the shutdown
		channelFactory.destroy();

		Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			// each channel should get ordered shutdown called
			verify(c1Spy).shutdown();
			verify(c2Spy).shutdown();
			verify(c3Spy).shutdown();

			// each channel should be awaitTermination (shortest grace periods first)
			var inOrder = inOrder(c1Spy, c2Spy, c3Spy);
			inOrder.verify(c2Spy).awaitTermination(anyLong(), eq(TimeUnit.MILLISECONDS));
			inOrder.verify(c1Spy).awaitTermination(anyLong(), eq(TimeUnit.MILLISECONDS));
			inOrder.verify(c3Spy).awaitTermination(anyLong(), eq(TimeUnit.MILLISECONDS));

			// c1 and c3 should never get forcibly shutdown
			// c2 is forcibly shutdown when exceedAwaitTime is true
			verify(c1Spy, never()).shutdownNow();
			verify(c3Spy, never()).shutdownNow();
			verify(c2Spy, times(exceedAwaitTime ? 1 : 0)).shutdownNow();
		});
	}

	private ManagedChannel setupSpy(ManagedChannel channel) throws InterruptedException {
		return this.setupSpy(channel, false);
	}

	private ManagedChannel setupSpy(ManagedChannel channel, boolean exceedAwaitTime) throws InterruptedException {
		var channelSpy = spy(channel);
		doAnswer((i) -> {
			try {
				Thread.sleep(3000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			if (!exceedAwaitTime) {
				return i.callRealMethod();
			}
			throw new InterruptedException("Exceeded await time");
		}).when(channelSpy).awaitTermination(anyLong(), eq(TimeUnit.MILLISECONDS));
		doReturn(false, true).when(channelSpy).isTerminated();
		return channelSpy;
	}

}
