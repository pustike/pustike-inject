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
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import javax.inject.Inject;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.NoSuchBindingException;
import io.github.pustike.inject.bind.InjectionPoint;

final class ExecutableInjectionPoint<T> implements InjectionPoint<T> {
    private final Executable executable;
    private final BindingTarget<?>[] bindingTargets;
    private boolean isStaticMethodInjected;

    ExecutableInjectionPoint(Executable executable) {
        this.executable = executable;
        this.bindingTargets = BindingTarget.createParameterTargets(executable);
    }

    static <T> InjectionPoint<T> create(Class<? extends T> targetType) {
        Constructor<?>[] constructors = targetType.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new RuntimeException("No constructors available for type: " + targetType);
        }
        Constructor<?> defaultConstructor = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                return new ExecutableInjectionPoint<>(constructor);
            }
            if (constructor.getParameterCount() == 0) {
                defaultConstructor = constructor;
            }
        }
        if (defaultConstructor == null) {
            throw new RuntimeException("No constructors available for type: " + targetType);
        }
        return new ExecutableInjectionPoint<>(defaultConstructor);
    }

    @Override
    public Object injectTo(T instance, Injector injector) throws NoSuchBindingException {
        if (isStaticMethodInjected) {
            return null; // do not invoke a static method more than once!
        }
        Object[] parameters = new Object[bindingTargets.length];
        for (int i = 0; i < parameters.length; i++) {
            BindingTarget<?> bindingTarget = bindingTargets[i];
            Optional<?> optional = injector.getIfPresent(bindingTarget.getKey());
            Object value = bindingTarget.isOptionalType() ? optional : optional.orElse(null);
            if (value == null && bindingTarget.isNotNullable()) {
                throw new NoSuchBindingException("Parameter key: " + bindingTarget.getKey()
                        + " doesn't allow null value in \n " + toString());
            }
            parameters[i] = value;
        }
        try {
            if (!executable.isAccessible()) {
                executable.setAccessible(true);
            }
            if (executable instanceof Constructor) {
                return ((Constructor) executable).newInstance(parameters);
            } else if (executable instanceof Method) {
                isStaticMethodInjected = Modifier.isStatic(executable.getModifiers());
                return ((Method) executable).invoke(instance, parameters);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("error when injecting dependency into " + toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        if (executable instanceof Constructor) {
            toStringBuilder.append("constructor=").append(executable);
        } else if (executable instanceof Method) {
            toStringBuilder.append("method=").append(executable);
        }
        for (BindingTarget<?> bindingTarget : bindingTargets) {
            toStringBuilder.append("\n -> ").append(bindingTarget.getKey());
        }
        return toStringBuilder.toString();
    }
}
