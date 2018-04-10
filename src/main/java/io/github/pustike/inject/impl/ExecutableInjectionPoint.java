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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.inject.Inject;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.NoSuchBindingException;
import io.github.pustike.inject.spi.InjectionPoint;

final class ExecutableInjectionPoint<T> implements InjectionPoint<T> {
    private final Executable executable;
    private final InjectionTarget<?>[] injectionTargets;
    private boolean isStaticMethodInjected;

    ExecutableInjectionPoint(Executable executable) {
        this.executable = executable;
        this.injectionTargets = InjectionTarget.createParameterTargets(executable);
        if (!executable.isAccessible()) {
            executable.setAccessible(true);
        }
    }

    static <T> InjectionPoint<T> create(Class<? extends T> targetType) {
        Constructor<?> defaultConstructor = null;
        for (Constructor<?> constructor : targetType.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                return new ExecutableInjectionPoint<>(constructor);
            } else if (constructor.getParameterCount() == 0) {
                defaultConstructor = constructor;
            }
        }
        if (defaultConstructor == null) {
            throw new RuntimeException("default constructor is not available for type: " + targetType);
        }
        return new ExecutableInjectionPoint<>(defaultConstructor);
    }

    @Override
    public Object injectTo(T instance, Injector injector) throws NoSuchBindingException {
        if (isStaticMethodInjected) {
            return null; // do not invoke a static method more than once!
        }
        Object[] parameters = new Object[injectionTargets.length];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = injectionTargets[i].getValue(injector);
        }
        try {
            if (executable instanceof Constructor) {
                return ((Constructor) executable).newInstance(parameters);
            } else if (executable instanceof Method) {
                isStaticMethodInjected = Modifier.isStatic(executable.getModifiers());
                return ((Method) executable).invoke(instance, parameters);
            }
            return null;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("error when injecting dependency into " + toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        if (executable instanceof Constructor) {
            toStringBuilder.append("constructor:\n").append(executable.getName());
        } else if (executable instanceof Method) {
            toStringBuilder.append("method:\n").append(executable.getName());
        }
        for (InjectionTarget<?> injectionTarget : injectionTargets) {
            toStringBuilder.append("\n -> ").append(injectionTarget.getKey());
        }
        return toStringBuilder.toString();
    }
}
