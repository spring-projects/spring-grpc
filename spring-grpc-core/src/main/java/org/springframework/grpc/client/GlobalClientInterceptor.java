/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.Order;

import io.grpc.ClientInterceptor;

/**
 * Annotation that can be specified on a gRPC {@link ClientInterceptor} bean which will
 * result in the interceptor being applied globally to channels.
 * <p>
 * The bean interceptor {@link Order} will be respected.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @author Chris Bono
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlobalClientInterceptor {

}
