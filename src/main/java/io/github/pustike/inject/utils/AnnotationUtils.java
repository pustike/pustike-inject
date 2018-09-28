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

import java.lang.annotation.Annotation;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Helper methods for working with {@link Annotation} instances. This class contains
 * various utility methods that make working with annotations simpler.
 * <p>
 * {@link Annotation} instances are always proxy objects; unfortunately
 * dynamic proxies cannot be depended upon to know how to implement certain
 * methods in the same manner as would be done by "natural" {@link Annotation}s.
 * The methods presented in this class can be used to avoid that possibility. It
 * is of course also possible for dynamic proxies to actually delegate their
 * e.g. {@link Annotation#equals(Object)}/{@link Annotation#hashCode()}/
 * {@link Annotation#toString()} implementations to {@link AnnotationUtils}.
 * <p>
 * From: commons-lang/src/main/java/org/apache/commons/lang3/AnnotationUtils.java
 */
public final class AnnotationUtils {
    /**
     * Generate a hash code for the given annotation type with its default attribute values
     * using the algorithm presented in the {@link Annotation#hashCode()} API docs.
     * @param annotationType the annotation type
     * @return the calculated hash code
     * @throws RuntimeException      if an {@code Exception} is encountered during annotation member access
     * @throws IllegalStateException if an annotation method invocation returns {@code null}
     */
    public static int hashCode(Class<? extends Annotation> annotationType) {
        int result = 0;
        for (Method method : annotationType.getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0 || method.getReturnType() == void.class) {
                continue;// continue if the supplied {@code method} is not an annotation attribute method.
            }
            try {
                Object value = method.getDefaultValue();
                if (value == null) {
                    throw new IllegalStateException(String.format("Annotation method %s returned null", method));
                }
                result += hashMember(method.getName(), value);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return result;
    }

    /**
     * Generate a hash code for the given annotation using the algorithm
     * presented in the {@link Annotation#hashCode()} API docs.
     * @param annotation the Annotation for a hash code calculation is desired, not {@code null}
     * @return the calculated hash code
     * @throws RuntimeException      if an {@code Exception} is encountered during annotation member access
     * @throws IllegalStateException if an annotation method invocation returns {@code null}
     */
    private static int hashCode(Annotation annotation) {
        int result = 0;
        Class<? extends Annotation> annotationType = annotation.annotationType();
        for (Method method : annotationType.getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0 || method.getReturnType() == void.class) {
                continue;// continue if the supplied {@code method} is not an annotation attribute method.
            }
            try {
                if (!method.trySetAccessible()) {
                    throw new InaccessibleObjectException("couldn't enable access to annotation method: " + method);
                }
                Object value = method.invoke(annotation);
                if (value == null) {
                    throw new IllegalStateException(String.format("Annotation method %s returned null", method));
                }
                result += hashMember(method.getName(), value);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return result;
    }

    /**
     * Helper method for generating a hash code for a member of an annotation.
     * @param name  the name of the member
     * @param value the value of the member
     * @return a hash code for this member
     */
    private static int hashMember(final String name, final Object value) {
        final int part1 = name.hashCode() * 127;
        if (value.getClass().isArray()) {
            return part1 ^ arrayMemberHash(value.getClass().getComponentType(), value);
        }
        if (value instanceof Annotation) {
            return part1 ^ hashCode((Annotation) value);
        }
        return part1 ^ value.hashCode();
    }

    /**
     * Helper method for generating a hash code for an array.
     * @param componentType the component type of the array
     * @param o             the array
     * @return a hash code for the specified array
     */
    private static int arrayMemberHash(final Class<?> componentType, final Object o) {
        if (componentType.equals(Byte.TYPE)) {
            return Arrays.hashCode((byte[]) o);
        }
        if (componentType.equals(Short.TYPE)) {
            return Arrays.hashCode((short[]) o);
        }
        if (componentType.equals(Integer.TYPE)) {
            return Arrays.hashCode((int[]) o);
        }
        if (componentType.equals(Character.TYPE)) {
            return Arrays.hashCode((char[]) o);
        }
        if (componentType.equals(Long.TYPE)) {
            return Arrays.hashCode((long[]) o);
        }
        if (componentType.equals(Float.TYPE)) {
            return Arrays.hashCode((float[]) o);
        }
        if (componentType.equals(Double.TYPE)) {
            return Arrays.hashCode((double[]) o);
        }
        if (componentType.equals(Boolean.TYPE)) {
            return Arrays.hashCode((boolean[]) o);
        }
        return Arrays.hashCode((Object[]) o);
    }

    private AnnotationUtils(){
    }
}
