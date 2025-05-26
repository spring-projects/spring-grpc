/*
 * Copyright (c) 2016-2023 The gRPC-Spring Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.grpc.client;

import io.grpc.ManagedChannel;

import org.springframework.util.Assert;

/**
 * This channel factory is a switch between the {@link InProcessGrpcChannelFactory} and an alternative implementation. All
 * channels that are configured with the {@code in-process} scheme will be handled by the in-process-channel-factory,
 * the other channels will be handled by the alternative implementation.
 *
 * <p>
 * <b>The following examples show how the configured address will be mapped to an actual channel:</b>
 * </p>
 *
 * <ul>
 * <li><code>in-process:foobar</code> -&gt; will use the <code>foobar</code> in-process-channel.</li>
 * <li><code>in-process:foo/bar</code> -&gt; will use the <code>foo/bar</code> in-process-channel.</li>
 * <li><code>static://127.0.0.1</code> -&gt; will be handled by the alternative grpc channel factory.</li>
 * </ul>
 *
 * <p>
 * Using this class does not incur any additional performance or resource costs, as the actual channels (in-process or
 * other) are only created on demand.
 * </p>
 */
public class InProcessOrAlternativeChannelFactory implements GrpcChannelFactory {

    private final InProcessGrpcChannelFactory inProcessChannelFactory;
    private final GrpcChannelFactory alternativeChannelFactory;

    /**
     * Creates a new InProcessOrAlternativeChannelFactory with the given properties and channel factories.
     *
     * @param inProcessChannelFactory the in process channel factory
     * @param alternativeChannelFactory the alternative channel factory
     */
    public InProcessOrAlternativeChannelFactory(InProcessGrpcChannelFactory inProcessChannelFactory,
            GrpcChannelFactory alternativeChannelFactory) {
        Assert.notNull(inProcessChannelFactory, "inProcessChannelFactory must not be null");
        Assert.notNull(alternativeChannelFactory, "alternativeChannelFactory must not be null");
        this.inProcessChannelFactory = inProcessChannelFactory;
        this.alternativeChannelFactory = alternativeChannelFactory;
    }

    @Override
    public ManagedChannel createChannel(String target, ChannelBuilderOptions options) {
        if (target.startsWith("in-process:")) {
            target = target.substring(11);
            return this.inProcessChannelFactory.createChannel(target, options);
        }
        return this.alternativeChannelFactory.createChannel(target, options);
    }

}
