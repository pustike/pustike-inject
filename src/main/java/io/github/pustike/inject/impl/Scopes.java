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

import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Provider;
import javax.inject.Singleton;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;

/**
 * Scopes supported by default.
 */
final class Scopes {
    /**
     * A binding with scope {@code PER_CALL} will create a new instance, whenever the binding is used to inject a value.
     */
    static final String PER_CALL = "PER_CALL"; // or PROTOTYPE

    static Scope createPerCallScope() {
        return new Scope() {
            @Override
            public <T> Provider<T> scope(BindingKey<T> bindingKey, Provider<T> creator) {
                return creator;
            }

            @Override
            public String toString() {
                return PER_CALL;
            }
        };
    }

    /**
     * A binding with scope {@code EAGER_SINGLETON} will create a single instance, which is injected whenever the
     * binding is used to inject a value. The instance will be created immediately.
     */
    static final String EAGER_SINGLETON = "EAGER_SINGLETON";

    static Scope createSingletonScope() {
        return new Scope() {
            @Override
            public <T> Provider<T> scope(BindingKey<T> bindingKey, Provider<T> creator) {
                // call @PreDestroy on instances that are stored in this scope.
                return new Provider<T>() {
                    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
                    private boolean isCreatingInstance;
                    private T instance;

                    @Override
                    public T get() {
                        if (instance == null) {
                            instance = createScopedInstance();
                            isCreatingInstance = false;
                        }
                        return instance;
                    }

                    private T createScopedInstance() {
                        rwl.writeLock().lock();
                        try {
                            if (instance != null) {
                                return instance;
                            }
                            if (isCreatingInstance) {// not to allow recursive invocations due to circular dependency!
                                throw new IllegalStateException(
                                        "can not create instance with circular dependency: " + bindingKey);
                            }
                            isCreatingInstance = true;
                            return creator.get();
                        } finally {
                            rwl.writeLock().unlock();
                        }
                    }
                };
            }

            @Override
            public String toString() {
                return Singleton.class.getName();
            }
        };
    }

    private Scopes() {
    }
}
