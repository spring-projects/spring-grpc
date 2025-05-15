/*
 * Copyright 2024-2024 the original author or authors.
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
package org.springframework.grpc.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.ClassFormatException;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;

public class ClasspathScanner implements ResourceLoaderAware {

	private static final Log logger = LogFactory.getLog(ClasspathScanner.class);

	static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

	private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

	private ResourcePatternResolver resourcePatternResolver;

	private MetadataReaderFactory metadataReaderFactory;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	public Set<Class<?>> scan(String basePackage, Class<?> type) {
		boolean debugEnabled = logger.isDebugEnabled();
		if (debugEnabled) {
			logger.debug("Scanning " + basePackage + " for classes of type " + type.getName());
		}
		return scan(basePackage, new AssignableTypeFilter(type));
	}

	public Set<Class<?>> annotated(String basePackage, Class<?> type) {
		boolean debugEnabled = logger.isDebugEnabled();
		if (debugEnabled) {
			logger.debug("Scanning " + basePackage + " for annotations of type " + type.getName());
		}
		@SuppressWarnings("unchecked")
		Class<? extends Annotation> annotationType = (Class<? extends Annotation>) type;
		return scan(basePackage, new AnnotationTypeFilter(annotationType));
	}

	public Set<Class<?>> scan(String basePackage, TypeFilter filter) {
		Set<Class<?>> candidates = new LinkedHashSet<>();
		if (filter == null) {
			return candidates;
		}
		boolean debugEnabled = logger.isDebugEnabled();
		boolean traceEnabled = logger.isTraceEnabled();
		try {
			String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
					+ resolveBasePackage(basePackage) + '/' + this.resourcePattern;
			Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);
			for (Resource resource : resources) {
				String filename = resource.getFilename();
				if (filename != null && filename.contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
					// Ignore CGLIB-generated classes in the classpath
					continue;
				}
				if (traceEnabled) {
					logger.trace("Scanning " + resource);
				}
				try {
					MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
					if (filter.match(metadataReader, getMetadataReaderFactory())) {
						Class<?> sbd = ClassUtils.forName(metadataReader.getClassMetadata().getClassName(), null);
						logger.debug("Identified candidate component class: " + resource);
						candidates.add(sbd);
					}
					else {
						if (debugEnabled) {
							logger.debug("Ignored because not a candidate class: " + resource);
						}
					}
				}
				catch (FileNotFoundException ex) {
					if (traceEnabled) {
						logger.trace("Ignored non-readable " + resource + ": " + ex.getMessage());
					}
				}
				catch (ClassFormatException ex) {
					if (debugEnabled) {
						logger.debug("Ignored incompatible class format in " + resource + ": " + ex.getMessage());
					}
				}
				catch (Throwable ex) {
					throw new BeanDefinitionStoreException("Failed to read candidate component class: " + resource, ex);
				}
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
		}
		return candidates;
	}

	private MetadataReaderFactory getMetadataReaderFactory() {
		if (this.metadataReaderFactory == null) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory();
		}
		return this.metadataReaderFactory;
	}

	private ResourcePatternResolver getResourcePatternResolver() {
		if (this.resourcePatternResolver == null) {
			this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
		}
		return this.resourcePatternResolver;
	}

	private String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(basePackage);
	}

}
