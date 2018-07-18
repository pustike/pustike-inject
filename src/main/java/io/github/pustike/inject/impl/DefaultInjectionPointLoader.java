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
package io.github.pustike.inject.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.inject.Inject;

import io.github.pustike.inject.spi.InjectionPoint;
import io.github.pustike.inject.spi.InjectionPointLoader;
import io.github.pustike.inject.utils.ReflectionUtils;

/**
 * Default Injection Point Loader.
 */
final class DefaultInjectionPointLoader implements InjectionPointLoader {
    private final Map<Class<?>, List<InjectionPoint<Object>>> injectionPointCache;

    DefaultInjectionPointLoader() {
        this.injectionPointCache = new ConcurrentHashMap<>();
    }

    @Override
    public List<InjectionPoint<Object>> getInjectionPoints(Class<?> clazz,
            Function<Class<?>, List<InjectionPoint<Object>>> creator) {
        return injectionPointCache.computeIfAbsent(clazz, creator);
    }

    @Override
    public void invalidateAll() {
        injectionPointCache.clear();
    }

    static List<InjectionPoint<Object>> doCreateInjectionPoints(final Class<?> targetClass) {
        List<InjectionPoint<Object>> injectionPointList = new LinkedList<>();
        Collection<Integer> visitedMethodHashCodes = new HashSet<>();
        for (Class<?> clazz = targetClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            int index = 0, staticIndex = 0;
            for (Field field : ReflectionUtils.getDeclaredFields(clazz)) {
                if (field.getDeclaredAnnotation(Inject.class) != null) {
                    final int idx = Modifier.isStatic(field.getModifiers()) ? staticIndex++ : index;
                    injectionPointList.add(idx, new FieldInjectionPoint<>(field));
                    index++;
                }
            }
            for (Method method : ReflectionUtils.getDeclaredMethods(clazz)) {
                int hashCode = computeHashCode(clazz, method);
                if (!visitedMethodHashCodes.contains(hashCode)) {
                    visitedMethodHashCodes.add(hashCode);
                    if (method.getDeclaredAnnotation(Inject.class) != null) {
                        final int idx = Modifier.isStatic(method.getModifiers()) ? staticIndex++ : index;
                        injectionPointList.add(idx, new ExecutableInjectionPoint<>(method));
                        index++;
                    }
                }
            }
        }
        return injectionPointList;
    }

    private static int computeHashCode(Class<?> clazz, Method method) {
        int hashCode = 31 + method.getName().hashCode();
        for (Class<?> parameterType : method.getParameterTypes()) {
            hashCode = 31 * hashCode + parameterType.hashCode();
        }
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)
                && !Modifier.isPrivate(modifiers)) { // package-private
            hashCode = 31 * hashCode + clazz.getPackage().hashCode();
        } else if (Modifier.isPrivate(modifiers)) {
            hashCode = 31 * hashCode + clazz.hashCode(); // private method
        }
        return hashCode;
    }
}
