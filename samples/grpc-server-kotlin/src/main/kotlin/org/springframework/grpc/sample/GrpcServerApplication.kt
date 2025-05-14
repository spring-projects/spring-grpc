package org.springframework.grpc.sample

import io.grpc.Status
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.grpc.server.exception.GrpcExceptionHandler

@SpringBootApplication
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
