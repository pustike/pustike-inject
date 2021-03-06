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
import java.lang.reflect.Method;
import jakarta.inject.Provider;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.spi.InjectionPoint;

/**
 * Provides new instance of the target for binding.
 */
final class InstanceProvider<T> implements Provider<T> {
    private final Class<? extends T> targetType;
    private final Executable executable;
    private final Object methodInstance;
    private final Class<?> providerType;
    private Provider<?> providerInstance;
    private Injector injector;
    private InjectionPoint<Object> injectionPoint;

    private InstanceProvider(Class<? extends T> targetType, Executable executable, Object methodInstance,
            Class<?> providerType) {
        this.targetType = targetType;
        this.executable = executable;
        this.methodInstance = methodInstance;
        this.providerType = providerType;
    }

    static <T> InstanceProvider<T> from(Class<? extends T> targetType) {
        return new InstanceProvider<>(targetType, null, null, null);
    }

    static <T> Provider<T> from(Constructor<? extends T> constructor) {
        return new InstanceProvider<>(null, constructor, null, null);
    }

    static <T> Provider<T> from(Method providerMethod, Object instance) {
        return new InstanceProvider<>(null, providerMethod, instance, null);
    }

    static <T> Provider<? extends T> fromProvider(Class<? extends Provider<? extends T>> providerType) {
        return new InstanceProvider<>(null, null, null, providerType);
    }

    void setInjector(Injector injector) {
        this.injector = injector;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        // if provider type is defined, return the instance from this provider
        // else create new instance from targetType or factory-constructor or factory-method
        return providerType != null ? (T) getProviderInstance().get()
                : (T) getInjectionPoint().injectTo(methodInstance, injector);
    }

    private Provider<?> getProviderInstance() {
        if(providerInstance == null) {// create the provider instance
            InjectionPoint<?> injectionPoint = ExecutableInjectionPoint.create(providerType);
            providerInstance = (Provider<?>) injectionPoint.injectTo(null, injector);
            injector.injectMembers(providerInstance);// and inject its members first
        }
        return providerInstance;
    }

    private InjectionPoint<Object> getInjectionPoint() {
        return injectionPoint == null ? injectionPoint = createInjectionPoint() : injectionPoint;
    }

    private InjectionPoint<Object> createInjectionPoint() {
        if (targetType != null) {
            return ExecutableInjectionPoint.create(targetType);
        } else if (executable != null) {
            return new ExecutableInjectionPoint<>(executable);
        }
        throw new RuntimeException("injection point can not be created using parameters provided!");
    }
}
