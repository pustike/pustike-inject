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
import javax.inject.Provider;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.InjectionPoint;

/**
 * Provides new instance of the target for binding.
 */
final class InstanceProvider<T> implements Provider<T> {
    private final Class<? extends T> targetType;
    private final Constructor<? extends T> constructor;
    private final Method factoryMethod;
    private final Class<? extends Provider<? extends T>> providerType;
    private Injector injector;
    private InjectionPoint<T> injectionPoint;
    private InjectionPoint<? extends Provider<? extends T>> providerInjectionPoint;

    private InstanceProvider(Class<? extends T> targetType, Constructor<? extends T> constructor, Method method,
            Class<? extends Provider<? extends T>> providerType) {
        this.targetType = targetType;
        this.constructor = constructor;
        this.factoryMethod = method;
        this.providerType = providerType;
    }

    static <T> InstanceProvider<T> from(Class<? extends T> targetType) {
        return new InstanceProvider<>(targetType, null, null, null);
    }

    static <T> Provider<T> from(Constructor<? extends T> constructor) {
        return new InstanceProvider<>(null, constructor, null, null);
    }

    static <T> Provider<T> from(Method factoryMethod) {
        return new InstanceProvider<>(null, null, factoryMethod, null);
    }

    static <T> Provider<? extends T> fromProvider(Class<? extends Provider<? extends T>> providerType) {
        return new InstanceProvider<>(null, null, null, providerType);
    }

    public void setInjector(Injector injector) {
        this.injector = injector;
    }

    @Override
    public T get() {
        try {
            // if provider type is defined,
            if (providerType != null) {
                // create the provider instance
                if (providerInjectionPoint == null) {
                    providerInjectionPoint = DefaultInjectionPointLoader.createInjectionPoint(providerType);
                }// and inject it's members first
                @SuppressWarnings("unchecked")
                Provider<T> provider = (Provider<T>) providerInjectionPoint.injectTo(null, injector);
                return provider.get();// return the instance from this provider
            } else {// create a new instance from targetType or factory-constructor or factory-method
                if (injectionPoint == null) {
                    injectionPoint = createInjectionPoint();
                }
                @SuppressWarnings("unchecked")
                T newInstance = (T) injectionPoint.injectTo(null, injector);
                return newInstance;
            }
        } catch (Throwable t) {
            throw new RuntimeException("error when creating provider", t);
        }
    }

    private InjectionPoint<T> createInjectionPoint() {
        if (targetType != null) {
            return DefaultInjectionPointLoader.createInjectionPoint(targetType);
        } else if (constructor != null) {
            return DefaultInjectionPointLoader.createInjectionPoint(constructor);
        } else if (factoryMethod != null) {
            return DefaultInjectionPointLoader.createInjectionPoint(factoryMethod);
        }
        throw new RuntimeException("injection point can not be created with the parameters provided!");
    }
}
