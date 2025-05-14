package org.springframework.grpc.sample

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.grpc.sample.proto.HelloRequest
import org.springframework.grpc.sample.proto.SimpleGrpc.SimpleBlockingStub
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest(
    properties = [
        "spring.grpc.server.port=0",
        "spring.grpc.client.default-channel.address=0.0.0.0:\${local.grpc.port}"
    ],
    useMainMethod = UseMainMethod.ALWAYS
)
@DirtiesContext
class GrpcServerApplicationTests {

    @Autowired
    private lateinit var stub: SimpleBlockingStub

    @Test
    fun contextLoads() {
    }

    @Test
    fun serverResponds() {
        log.info("Testing")
        val response = stub.sayHello(
            HelloRequest.newBuilder()
                .setName("Alien")
                .build()
        )
        Assertions.assertEquals("Hello ==> Alien", response.getMessage())
    }

    companion object {
        private val log: Log = LogFactory.getLog(GrpcServerApplicationTests::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(GrpcServerApplication::class.java).run()
        }
    }
}
