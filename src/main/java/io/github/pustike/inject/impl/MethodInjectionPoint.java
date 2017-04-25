/*
 * Copyright (C) 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.inject.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.InjectionPoint;

final class MethodInjectionPoint<T> implements InjectionPoint<T> {
    private final BindingKey<T>[] targetKeys;
    private final Boolean[] nullableParams;
    private final Constructor<?> constructor;
    private final Method method;
    private boolean isStaticMethodInjected;

    MethodInjectionPoint(Constructor constructor, BindingKey<T>[] targetKeys, Boolean[] nullableParams) {
        this.constructor = constructor;
        this.method = null;
        this.targetKeys = targetKeys;
        this.nullableParams = nullableParams;
    }

    MethodInjectionPoint(Method method, BindingKey<T>[] targetKeys, Boolean[] nullableParams) {
        this.constructor = null;
        this.method = method;
        this.targetKeys = targetKeys;
        this.nullableParams = nullableParams;
    }

    @Override
    public Object injectTo(T instance, Injector injector) {
        try {
            Object[] parameters = new Object[targetKeys.length];
            for (int i = 0; i < parameters.length; i++) {
                BindingKey<T> targetKey = targetKeys[i];
                Object value = targetKey != null ? injector.getInstance(targetKey) : null;
                if (value == null && nullableParams[i] != null && !nullableParams[i]) {
                    throw new NullPointerException("Parameter key: " + targetKey + //
                            " doesn't allow null value in \n " + toString());
                }
                parameters[i] = value;
            }
            if (constructor != null) {
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }
                return constructor.newInstance(parameters);
            } else if (method != null) {
                if (isStaticMethodInjected) {
                    return null; // do not invoke a static method more than once!
                }
                isStaticMethodInjected = Modifier.isStatic(method.getModifiers());
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method.invoke(instance, parameters);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("error when injecting dependency into " + toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        if (constructor != null) {
            toStringBuilder.append("constructor=").append(constructor);
        } else if (method != null) {
            toStringBuilder.append("method=").append(method);
        }
        for (BindingKey<T> targetKey : targetKeys) {
            toStringBuilder.append("\n -> ").append(targetKey);
        }
        return toStringBuilder.toString();
    }
}
