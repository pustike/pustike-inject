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
package io.github.pustike.inject.scope;

import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Provider;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;

/**
 * Thread scope, backed by a {@link java.lang.ThreadLocal}.
 * Example usage:
 * <pre>{@code
 * Injector injector = Injectors.create((Module) binder -> {
 *     ThreadScope threadScope = new ThreadScope();
 *     binder.bindScope(ThreadScoped.class, threadScope);
 *     binder.bind(ThreadScope.class).toInstance(threadScope);
 *     binder.bind(SomeClass.class).in(threadScope);
 * });
 * }</pre>
 * In thread pooling scenario's, never forget to reset the scope at the end of a request:
 * <pre>{@code
 * injector.getInstance(ThreadScope.class).clearContext();
 * }</pre>
 * Note that if you create new threads within this scope, they will start with a clean slate.
 */
public final class ThreadScope implements Scope {
    // use lazy init to avoid memory overhead when not using the scope?
    private static final ThreadLocal<ThreadScopeContext> threadLocal = ThreadLocal.withInitial(ThreadScopeContext::new);

    private static ThreadScopeContext getContext() {
        return threadLocal.get();
    }

    /**
     * Execute this if you plan to reuse the same thread, e.g. in a servlet environment threads might get reused.
     * Preferably, call this method in a finally block to make sure that it executes, to avoid possible memory leaks.
     */
    public static void clearContext() {
        threadLocal.remove();
    }

    @Override
    public <T> Provider<T> scope(BindingKey<T> bindingKey, Provider<T> creator) {
        return () -> {
            ThreadScopeContext context = getContext();
            T value = context.get(bindingKey);
            if (value == null) {
                value = creator.get();
                context.put(bindingKey, value);
            }
            return value;
        };
    }

    @Override
    public String toString() {
        return ThreadScoped.class.getName();
    }

    /**
     * Cache class for type capture and minimizing ThreadLocal lookups.
     */
    private static final class ThreadScopeContext {
        private final Map<BindingKey<?>, Object> map;

        private ThreadScopeContext() {
            map = new HashMap<>();
        }

        // suppress warnings because the add method captures the type
        @SuppressWarnings("unchecked")
        <T> T get(BindingKey<T> key) {
            return (T) map.get(key);
        }

        <T> void put(BindingKey<T> key, T value) {
            map.put(key, value);
        }
    }
}
