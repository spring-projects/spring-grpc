/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.sample

import org.springframework.grpc.sample.proto.HelloReply
import org.springframework.grpc.sample.proto.HelloRequest
import org.springframework.grpc.sample.proto.SimpleGrpcKt
import org.springframework.stereotype.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Service
class GrpcServerService : SimpleGrpcKt.SimpleCoroutineImplBase() {

    override suspend fun sayHello(request: HelloRequest): HelloReply {

        if (request.name.startsWith("error")) {
            throw IllegalArgumentException("Bad name: ${request.name}")
        }

        if (request.name.startsWith("internal")) {
            throw RuntimeException()
        }

        return HelloReply.newBuilder()
            .setMessage("Hello ==> ${request.name}")
            .build()
    }

    override fun streamHello(request: HelloRequest): Flow<HelloReply> {
        return (1..10)
            .map { HelloReply.newBuilder().setMessage("Hello ($it)  ==> ${request.name}").build() }
            .asFlow()
    }
}
