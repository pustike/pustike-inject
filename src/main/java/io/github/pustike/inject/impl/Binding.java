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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        this.bindingKey = Objects.requireNonNull(bindingKey);
        this.provider = Objects.requireNonNull(provider);
        this.scope = Objects.requireNonNull(scope);
    }

    Binding(BindingKey<T> bindingKey, List<Binding<T>> bindingList, Scope scope) {
        this(bindingKey, new MultiBindingProvider<>(bindingKey, bindingList), scope);
    }

    boolean addBinding(Binding<T> binding) {
        return provider instanceof MultiBindingProvider && ((MultiBindingProvider<T>) provider).addBinding(binding);
    }

    void postConfiguration(DefaultInjector injector) {
        if (provider instanceof MultiBindingProvider) {
            ((MultiBindingProvider) provider).setInjector(injector);
        } else {
            this.injector = injector;
            if (provider instanceof InstanceProvider) {
                ((InstanceProvider) provider).setInjector(injector);
                this.providerInjected = true;// internal instanceProvider doesn't require any dependency injection
            }
            this.scopedProvider = scope.scope(bindingKey, this::createInstance);
            if (scope.toString().equals(Scopes.EAGER_SINGLETON)) {
                this.scopedProvider.get();
            }
        }
    }

    Object getInstance(BindingKey<?> targetKey) {
        if (provider instanceof MultiBindingProvider) {
            return ((MultiBindingProvider<?>) provider).getInstance(targetKey);
        }
        return targetKey.isProviderKey() ? this.scopedProvider : this.scopedProvider.get();
    }

    private T createInstance() {
        // inject dependencies into the provider instance as well!
        if (!providerInjected) {
            injector.injectMembers(provider);
            providerInjected = true;
        }
        // get an instance from the provider
        T newInstance = provider.get();
        if (newInstance != null) {// inject members of the new instance
            injector.injectMembers(bindingKey, newInstance);
        }
        return newInstance;
    }

    private static final class MultiBindingProvider<T> implements Provider<T> {
        private final BindingKey<T> bindingKey;
        private final List<Binding<T>> bindingList;

        private MultiBindingProvider(BindingKey<T> bindingKey, List<Binding<T>> bindingList) {
            this.bindingKey = bindingKey;
            this.bindingList = bindingList;
        }

        private boolean addBinding(Binding<T> binding) {
            return bindingList.add(binding);
        }

        private void setInjector(DefaultInjector injector) {
            for (Binding<T> binding : bindingList) {
                binding.postConfiguration(injector);
            }
        }

        private Collection<?> getInstance(BindingKey<?> targetKey) {
            List<Object> instanceList = new ArrayList<>();
            for (Binding<T> binding : bindingList) {
                instanceList.add(binding.getInstance(targetKey));
            }
            return Collections.unmodifiableList(instanceList);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) getInstance(bindingKey);
        }
    }
}
