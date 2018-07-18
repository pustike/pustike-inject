/*
 * Copyright (C) 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.inject.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Reflection Utilities.
 */
public final class ReflectionUtils {
    /**
     * A comparator suitable for comparing two classes if they are loaded by the same classloader.
     * <p>Within a single classloader there can only be one class with a given name, so we just compare the names.
     */
    private static final Comparator<Class<?>> classComparator = Comparator.comparing(Class::getName);
    /**
     * A comparator suitable for comparing two fields if they are owned by the same class.
     * <p>Within a single class it is sufficient to compare the non-generic field signature which consists of the
     * field name and type.
     */
    private static final Comparator<Field> fieldComparator = Comparator.comparing(Field::getName)
            .thenComparing(Field::getType, classComparator);
    /**
     * A comparator suitable for comparing two methods if they are owned by the same class.
     * <p>Within a single class it is sufficient to compare the non-generic method signature which consists of name,
     * return type and parameter types.
     */
    private static final Comparator<Method> methodComparator = Comparator.comparing(Method::getName)
            .thenComparing(Method::getReturnType, classComparator).thenComparing(method ->
                    Arrays.asList(method.getParameterTypes()), createLexicalOrderComparator(classComparator));

    // An ordering which sorts iterables by comparing corresponding elements pairwise.
    private static <T> Comparator<Iterable<T>> createLexicalOrderComparator(Comparator<T> elementComparator) {
        return (o1, o2) -> {
            Iterator<T> left = o1.iterator(), right = o2.iterator();
            while (left.hasNext()) {
                if (!right.hasNext()) {
                    return 1; // because it's longer
                }
                int result = elementComparator.compare(left.next(), right.next());
                if (result != 0) {
                    return result;
                }
            }
            if (right.hasNext()) {
                return -1; // because it's longer
            }
            return 0;
        };
    }

    /**
     * Fields in the returned array from {@link Class#getDeclaredFields()}, are not sorted and are not in any
     * particular order. Additionally, Some JVMs actually randomize the order from run to run.
     * @param clazz the declaring class
     * @return the array of Field objects
     */
    public static Field[] getDeclaredFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Arrays.sort(declaredFields, fieldComparator);
        return declaredFields;
    }

    /**
     * Methods in the returned array from {@link Class#getDeclaredMethods()}, are not sorted and are not in any
     * particular order. Additionally, Some JVMs actually randomize the order from run to run.
     * @param clazz the declaring class
     * @return the array of Method objects
     */
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        Arrays.sort(declaredMethods, methodComparator);
        return declaredMethods;
    }

    private ReflectionUtils() {
    }
}
