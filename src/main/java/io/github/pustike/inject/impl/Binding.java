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

    Binding(BindingKey<T> bindingKey, Provider<T> provider, Scope scope) {
        this.bindingKey = bindingKey;
        this.provider = provider;
        this.scope = scope;
    }

    private Provider<T> getScopedProvider() {
        if (scopedProvider == null) {
            scopedProvider = scope.scope(bindingKey, this::createInstance);
        }
        return scopedProvider;
    }

    Object getInstance(BindingKey<?> targetKey) {
        return targetKey.isProviderKey() ? getScopedProvider() : getScopedProvider().get();
    }

    void postConfiguration(DefaultInjector injector) {
        this.injector = injector;
        if (scope.toString().equals(Scopes.EAGER_SINGLETON)) {
            getScopedProvider().get();
        }
    }

    private T createInstance() {
        // inject dependencies into the provider instance as well!
        if (!(provider instanceof InstanceProvider)) {
            // inject members into provider, every time an instance is created
            // because they could be of different scope!
            injector.injectMembers(provider);
        }
        // get an instance from the provider
        T newInstance = provider.get();
        // if not an injector binding, inject members of the instance
        if (newInstance != null && !injector.equals(newInstance)) {
            injector.injectMembers(bindingKey, newInstance);
        }
        return newInstance;
    }
}
