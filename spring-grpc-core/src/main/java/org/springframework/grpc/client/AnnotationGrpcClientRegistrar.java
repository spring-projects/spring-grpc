/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.util.HashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;
import org.springframework.util.ClassUtils;

public class AnnotationGrpcClientRegistrar extends AbstractGrpcClientRegistrar {

	@Override
	protected GrpcClientRegistrationSpec[] collect(AnnotationMetadata meta) {
		Set<AnnotationAttributes> attrs = meta.getMergedRepeatableAnnotationAttributes(ImportGrpcClients.class,
				ImportGrpcClients.Container.class, false);
		Set<GrpcClientRegistrationSpec> specs = new HashSet<>();
		for (AnnotationAttributes attr : attrs) {
			specs.add(register(meta, attr));
		}
		return specs.toArray(new GrpcClientRegistrationSpec[0]);
	}

	private GrpcClientRegistrationSpec register(AnnotationMetadata meta, AnnotationAttributes attr) {
		String target = attr.getString("target");
		String prefix = attr.getString("prefix");
		Class<?>[] types = attr.getClassArray("types");
		Class<?>[] basePackageClasses = attr.getClassArray("basePackageClasses");
		String[] basePackages = attr.getStringArray("basePackages");
		Class<? extends StubFactory<?>> factory = attr.getClass("factory");
		if (factory == UnspecifiedStubFactory.class) {
			factory = null;
		}
		if (types.length == 0 && basePackageClasses.length == 0 && basePackages.length == 0) {
			basePackages = new String[] { ClassUtils.getPackageName(meta.getClassName()) };
		}
		return GrpcClientRegistrationSpec.of(target)
			.factory(factory)
			.types(types)
			.packages(basePackages)
			.packageClasses(basePackageClasses)
			.prefix(prefix);
	}

}
