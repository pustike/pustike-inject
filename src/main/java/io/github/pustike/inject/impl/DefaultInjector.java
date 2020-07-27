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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import jakarta.inject.Provider;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.NoSuchBindingException;
import io.github.pustike.inject.bind.Module;
import io.github.pustike.inject.spi.InjectionListener;
import io.github.pustike.inject.spi.InjectionPoint;
import io.github.pustike.inject.spi.InjectionPointLoader;

/**
 * Default implementation of an {@link Injector injector}.
 */
public final class DefaultInjector implements Injector {
    private final Map<BindingKey<?>, Binding<?>> keyBindingMap;
    private final InjectionPointLoader injectionPointLoader;
    private final Function<Class<?>, List<InjectionPoint<Object>>> injectionPointCreator;
    private final Map<InjectionListener, Predicate<Class<?>>> injectionListenerMatcherMap;
    private DefaultInjector parentInjector;
    private boolean configured;

    /**
     * Create an instance of the Injector with bindings provided by modules and using an internal cache.
     * @param modules a collection of modules
     * @return the instance of default injector
     */
    public static Injector create(Iterable<Module> modules) {
        return create(null, modules);
    }

    /**
     * Create an instance of the Injector with bindings provided by modules and the function to apply for injection
     * point cache.
     * @param injectionPointLoader injection point cache/mapping function
     * @param modules a collection of modules
     * @return the instance of default injector
     */
    public static DefaultInjector create(InjectionPointLoader injectionPointLoader, Iterable<Module> modules) {
        Objects.requireNonNull(modules);
        if (modules instanceof Collection ? ((Collection<?>) modules).isEmpty()
                : !modules.iterator().hasNext()) {
            throw new IllegalArgumentException("The module list must not be empty.");
        }
        DefaultInjector injector = new DefaultInjector(injectionPointLoader);
        DefaultBinder binder = new DefaultBinder(injector);
        // add injector itself as a binding to the registry
        BindingKey<Injector> bindingKey = BindingKey.of(Injector.class);
        injector.register(bindingKey, new Binding<>(bindingKey, () -> injector,
                binder.getScope(Scopes.SINGLETON), injector));
        binder.configure(modules);
        // do not allow any further modifications to keyBindingMap
        injector.configured = true;
        injector.keyBindingMap.values().forEach(Binding::createIfEagerSingleton);
        binder.clear();// clear them all
        return injector;
    }

    private DefaultInjector(InjectionPointLoader injectionPointLoader) {
        this.keyBindingMap = new ConcurrentHashMap<>();
        this.injectionPointLoader = injectionPointLoader == null //
                ? new DefaultInjectionPointLoader() : injectionPointLoader;
        this.injectionPointCreator = DefaultInjectionPointLoader::doCreateInjectionPoints;
        this.injectionListenerMatcherMap = new LinkedHashMap<>();
    }

    @Override
    public <T> T getInstance(Class<T> type) throws NoSuchBindingException {
        return getInstance(BindingKey.of(type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(BindingKey<T> key) throws NoSuchBindingException {
        Binding<T> binding = getBinding(key);
        if (binding == null) {
            throw new NoSuchBindingException("No binding registered for key: " + key);
        }
        return (T) binding.getInstance(key);
    }

    @Override
    public <T> Optional<T> getIfPresent(Class<T> type) {
        return getIfPresent(BindingKey.of(type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getIfPresent(BindingKey<T> key) {
        Binding<T> binding = getBinding(key);
        return binding == null ? Optional.empty() : Optional.ofNullable((T) binding.getInstance(key));
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type) throws NoSuchBindingException {
        return getInstance(BindingKey.of(type).toProviderType());
    }

    @Override
    public <T> Provider<T> getProvider(BindingKey<T> key) throws NoSuchBindingException {
        return getInstance(key.toProviderType());
    }

    @Override
    public void injectMembers(Object instance) {
        Objects.requireNonNull(instance);
        @SuppressWarnings("unchecked")
        Class<Object> instanceClass = (Class<Object>) instance.getClass();
        injectMembers(BindingKey.of(instanceClass), instance);
    }

    @Override
    public Injector getParent() {
        return parentInjector;
    }

    @Override
    public Injector createChildInjector(Module... modules) {
        return createChildInjector(List.of(modules));
    }

    @Override
    public Injector createChildInjector(Iterable<Module> modules) {
        DefaultInjector injector = create(injectionPointLoader, modules);
        injector.parentInjector = this;
        return injector;
    }

    private <T> Binding<T> getBinding(BindingKey<T> bindingKey) {
        if (!configured) {
            throw new IllegalStateException("Bindings can be obtained only after the Injector is fully configured!");
        }
        @SuppressWarnings("unchecked")
        Binding<T> binding = bindingKey != null ? (Binding<T>) keyBindingMap.get(bindingKey) : null;
        if (binding == null && parentInjector != null) {
            binding = parentInjector.getBinding(bindingKey);
        }
        return binding;
    }

    <T> void register(BindingKey<T> bindingKey, Binding<T> binding) {
        if (configured) {
            throw new IllegalStateException("Bindings can not be registered after the Injector is configured!");
        }
        @SuppressWarnings("unchecked")
        Binding<T> previousBinding = (Binding<T>) keyBindingMap.putIfAbsent(bindingKey, binding);
        if (previousBinding != null) {
            if(!previousBinding.addBinding(binding)) {// if multiBinder add this binding to it!
                throw new IllegalStateException("A binding is already registered for this key: " + bindingKey);
            }
        }
    }

    void bindInjectionListener(Predicate<Class<?>> typeMatcher, InjectionListener injectionListener) {
        injectionListenerMatcherMap.put(injectionListener, typeMatcher);
    }

    <T> void injectMembers(BindingKey<T> bindingKey, T instance) {
        final Class<?> instanceType = instance.getClass();
        // first inject based on all known bindings
        List<InjectionPoint<Object>> injectionPointList = injectionPointLoader
                .getInjectionPoints(instanceType, injectionPointCreator);
        for (InjectionPoint<Object> injectionPoint : injectionPointList) {
            injectionPoint.injectTo(instance, this);
        }
        // call matching Injection Listeners for this instance type
        for (Map.Entry<InjectionListener, Predicate<Class<?>>> mapEntry : injectionListenerMatcherMap.entrySet()) {
            Predicate<Class<?>> typeMatcher = mapEntry.getValue();
            if (typeMatcher.test(instanceType)) {
                mapEntry.getKey().afterInjection(bindingKey, instance);
            }
        }
    }

    public void dispose() {
        keyBindingMap.clear();
        injectionPointLoader.invalidateAll();
        injectionListenerMatcherMap.clear();
        parentInjector = null;
    }
}
