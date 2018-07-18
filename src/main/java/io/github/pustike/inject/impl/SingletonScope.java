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

import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Provider;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;

/**
 * Singleton Scope.
 */
final class SingletonScope implements Scope {
    private final boolean eagerSingleton;

    SingletonScope() {
        this(false);
    }

    SingletonScope(boolean eagerSingleton) {
        this.eagerSingleton = eagerSingleton;
    }

    @Override
    public <T> Provider<T> scope(BindingKey<T> bindingKey, Provider<T> creator) {
        return new SingletonProvider<>(bindingKey, creator);
    }

    @Override
    public String toString() {
        return eagerSingleton ? Scopes.EAGER_SINGLETON : Scopes.SINGLETON;
    }

    private static final class SingletonProvider<T> implements Provider<T> {
        /** A sentinel value representing null. */
        private static final Object NULL = new Object();
        private final BindingKey<T> bindingKey;
        private final Provider<T> creator;
        private final ReentrantReadWriteLock rwl;
        private boolean isCreatingInstance;
        private volatile Object instance;

        SingletonProvider(BindingKey<T> bindingKey, Provider<T> creator) {
            this.bindingKey = bindingKey;
            this.creator = creator;
            this.rwl = new ReentrantReadWriteLock();
        }

        @Override
        public T get() {
            // cache volatile variable for the usual case of already initialized object
            final Object initialInstance = instance;
            if (initialInstance == null) {
                rwl.writeLock().lock();
                try {
                    // intentionally reread volatile variable to prevent double initialization
                    if (instance == null) {
                        if (isCreatingInstance) {
                            throw new IllegalStateException(
                                    "can not create instance with circular dependency: " + bindingKey);
                        }
                        isCreatingInstance = true;
                        T provided = creator.get();
                        instance = provided == null ? NULL : provided;
                    }
                    // caching volatile variable to minimize number of reads performed
                    final Object initializedInstance = instance;
                    @SuppressWarnings("unchecked")
                    T typedInstance = (T) initializedInstance;
                    return initializedInstance == NULL ? null : typedInstance;
                } finally {
                    isCreatingInstance = false;
                    rwl.writeLock().unlock();
                }
            } else {// singleton is already initialized and local cache can be used
                @SuppressWarnings("unchecked")
                T typedInstance = (T) initialInstance;
                return initialInstance == NULL ? null : typedInstance;
            }
        }
    }
}
