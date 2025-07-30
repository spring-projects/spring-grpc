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
package org.springframework.grpc.internal;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

/**
 * Convenience methods performing bean lookups that are not currently provided by the
 * standard Spring facilities.
 * <p>
 * <b>NOTE:</b> This is considered an internal non-public API despite its public method
 * accessors.
 *
 * @author Chris Bono
 */
public final class ApplicationContextBeanLookupUtils {

	private ApplicationContextBeanLookupUtils() {
	}

	/**
	 * Find all beans of the given type which may be annotated with the supplied
	 * {@link Annotation} type and returns a map of matching bean instances with the
	 * annotation instance (or null if the bean is not annotated with the specified
	 * annotation type).
	 * <p>
	 * Note that the map is ordered according to the matching beans {@link Order}
	 * annotation.
	 * <p>
	 * Note that this method considers objects created by FactoryBeans, which means that
	 * FactoryBeans will get initialized in order to determine their object type.
	 * @param applicationContext the application context
	 * @param beanType the type of bean
	 * @param annotationType the type of annotation
	 * @param <B> type of bean
	 * @param <A> type of annotation
	 * @return a map of matching bean instances with the annotation instance (or null)
	 * ordered according to their {@link Order} with annotation
	 */
	public static <B, A extends Annotation> LinkedHashMap<B, A> getOrderedBeansWithAnnotation(
			ApplicationContext applicationContext, Class<B> beanType, Class<A> annotationType) {
		Assert.notNull(applicationContext, () -> "applicationContext must not be null");
		var annotatedBeanNamesToBeans = applicationContext.getBeansWithAnnotation(annotationType);
		var annotatedBeansToBeanNames = new LinkedHashMap<Object, String>();
		annotatedBeanNamesToBeans.forEach((name, bean) -> annotatedBeansToBeanNames.put(bean, name));
		var orderedBeans = new LinkedHashMap<B, A>();
		applicationContext.getBeanProvider(beanType).orderedStream().forEachOrdered((b) -> {
			var beanName = annotatedBeansToBeanNames.get(b);
			A beanAnnotation = null;
			if (beanName != null) {
				beanAnnotation = applicationContext.findAnnotationOnBean(beanName, annotationType);
			}
			orderedBeans.put(b, beanAnnotation);
		});
		return orderedBeans;
	}

	/**
	 * Find all beans which are annotated with the supplied {@link Annotation} type.
	 * <p>
	 * Note that this method considers objects created by FactoryBeans, which means that
	 * FactoryBeans will get initialized in order to determine their object type.
	 * @param applicationContext the application context
	 * @param beanType the type of bean
	 * @param annotationType the type of annotation
	 * @param <B> type of bean
	 * @param <A> type of annotation
	 * @return a list of the matching beans ordered according to their {@link Order}
	 * annotation
	 */
	public static <B, A extends Annotation> List<B> getBeansWithAnnotation(ApplicationContext applicationContext,
			Class<B> beanType, Class<A> annotationType) {
		Assert.notNull(applicationContext, () -> "applicationContext must not be null");
		var nameToBeanMap = applicationContext.getBeansWithAnnotation(annotationType);
		var beanToNameMap = new LinkedHashMap<Object, String>();
		nameToBeanMap.forEach((name, bean) -> beanToNameMap.put(bean, name));
		return applicationContext.getBeanProvider(beanType).orderedStream().filter(beanToNameMap::containsKey).toList();
	}

	/**
	 * Sorts the supplied list in place using an {@link OrderComparator} that takes the
	 * {@link Order @Order} annotation on bean factory methods in configuration classes
	 * into account.
	 * @param applicationContext the application context
	 * @param beanType the type of beans in the list
	 * @param beans the list of beans to sort
	 */
	public static void sortBeansIncludingOrderAnnotation(ApplicationContext applicationContext, Class<?> beanType,
			List<?> beans) {
		var beanToNameMap = new LinkedHashMap<Object, String>();
		applicationContext.getBeansOfType(beanType).forEach((name, bean) -> beanToNameMap.put(bean, name));
		beans.sort(OrderComparator.INSTANCE.withSourceProvider(bean -> {
			Integer priority = AnnotationAwareOrderComparator.INSTANCE.getPriority(bean);
			if (priority != null) {
				return (Ordered) () -> priority;
			}
			// Consult the bean factory method for annotations
			String beanName = beanToNameMap.get(bean);
			if (beanName != null) {
				Order order = applicationContext.findAnnotationOnBean(beanName, Order.class);
				if (order != null) {
					return (Ordered) order::value;
				}
			}
			return null;
		}));
	}

}
