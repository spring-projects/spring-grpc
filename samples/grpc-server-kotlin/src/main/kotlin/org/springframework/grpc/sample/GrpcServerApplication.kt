/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.sample

import io.grpc.Status
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.sample.proto.SimpleGrpc;

@SpringBootApplication
@ImportGrpcClients(basePackageClasses = [SimpleGrpc::class])
open class GrpcServerApplication {

    @Bean
    open fun globalInterceptor(): GrpcExceptionHandler = GrpcExceptionHandler { exception ->
        when (exception) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(exception.message).asException();
            else -> null
        }
    }

}


fun main(args: Array<String>) {
    runApplication<GrpcServerApplication>(*args)
}
