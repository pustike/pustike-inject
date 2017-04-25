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

import javax.inject.Provider;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;

/**
 * Binding.
 */
final class Binding<T> {
    private final BindingKey<T> bindingKey;
    private final Provider<T> provider;
    private final Scope scope;
    private DefaultInjector injector;
    private Provider<T> scopedProvider;
    private boolean providerInjected;

    Binding(BindingKey<T> bindingKey, Provider<T> provider, Scope scope) {
        this.bindingKey = bindingKey;
        this.provider = provider;
        this.scope = scope;
        // internal instanceProvider doesn't require any dependency injection
        this.providerInjected = provider instanceof InstanceProvider;
    }

    private Provider<T> getScopedProvider() {
        return scopedProvider;
    }

    Object getInstance(BindingKey<?> targetKey) {
        return targetKey.isProviderKey() ? getScopedProvider() : getScopedProvider().get();
    }

    void postConfiguration(DefaultInjector injector) {
        this.injector = injector;
        this.scopedProvider = scope.scope(bindingKey, this::createInstance);
        if (scope.toString().equals(Scopes.EAGER_SINGLETON)) {
            getScopedProvider().get();
        }
    }

    private T createInstance() {
        // inject dependencies into the provider instance as well!
        if (!providerInjected) {
            injector.injectMembers(provider);
            providerInjected = true;
        }
        // get an instance from the provider
        T newInstance = provider.get();
        if (newInstance != null ) {// inject members of the new instance
            injector.injectMembers(bindingKey, newInstance);
        }
        return newInstance;
    }
}
