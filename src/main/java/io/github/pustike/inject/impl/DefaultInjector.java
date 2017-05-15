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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.inject.Provider;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.NoSuchBindingException;
import io.github.pustike.inject.bind.InjectionListener;
import io.github.pustike.inject.bind.InjectionPoint;
import io.github.pustike.inject.bind.InjectionPointLoader;
import io.github.pustike.inject.bind.Module;

/**
 * Default implementation of an {@link Injector injector}.
 */
public final class DefaultInjector implements Injector {
    private final InjectionPointLoader injectionPointLoader;
    private final Map<BindingKey<?>, Binding<?>> keyBindingMap;
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
        if (modules == null) {
            throw new NullPointerException("The module list must not be null.");
        }
        if (modules instanceof Collection ? ((Collection<?>) modules).isEmpty()
                : !modules.iterator().hasNext()) {
            throw new IllegalArgumentException("The module list must not be empty.");
        }
        DefaultInjector injector = new DefaultInjector(injectionPointLoader);
        DefaultBinder binder = new DefaultBinder(injector).configure(modules);
        // add injector itself as a binding to the registry
        BindingKey<Injector> bindingKey = BindingKey.of(Injector.class);
        Provider<Injector> injectorProvider = () -> injector;
        injector.register(bindingKey, new Binding<>(bindingKey, injectorProvider,
                binder.getScope(Scopes.EAGER_SINGLETON)));
        // do not allow any further modifications to keyBindingMap
        injector.postConfiguration();
        binder.clear();// clear them all
        return injector;
    }

    private DefaultInjector(InjectionPointLoader injectionPointLoader) {
        this.injectionPointLoader = injectionPointLoader == null //
                ? new DefaultInjectionPointLoader() : injectionPointLoader;
        this.keyBindingMap = new ConcurrentHashMap<>();
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
    public <T> Provider<T> getProvider(Class<T> type) throws NoSuchBindingException {
        return getProvider(BindingKey.of(type).createProviderKey());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Provider<T> getProvider(BindingKey<T> key) throws NoSuchBindingException {
        return (Provider<T>) getInstance(key.isProviderKey() ? key : key.createProviderKey());
    }

    @Override
    public void injectMembers(Object instance) {
        if (instance == null) {
            throw new NullPointerException("The instance must not be null.");
        }
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
        return createChildInjector(Arrays.asList(modules));
    }

    @Override
    public Injector createChildInjector(Iterable<Module> modules) {
        DefaultInjector injector = create(injectionPointLoader, modules);
        injector.parentInjector = this;
        return injector;
    }

    @SuppressWarnings("unchecked")
    private <T> Binding<T> getBinding(BindingKey<T> bindingKey) {
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
        keyBindingMap.put(bindingKey, binding);
    }

    void bindInjectionListener(Predicate<Class<?>> typeMatcher, InjectionListener injectionListener) {
        injectionListenerMatcherMap.put(injectionListener, typeMatcher);
    }

    private void postConfiguration() {
        this.configured = true;
        for (Binding<?> binding : keyBindingMap.values()) {
            binding.postConfiguration(this);
        }
    }

    <T> void injectMembers(BindingKey<T> bindingKey, T instance) {
        // first inject based on all known bindings
        List<InjectionPoint<Object>> injectionPointList = injectionPointLoader.getInjectionPoints(instance.getClass());
        for (InjectionPoint<Object> injectionPoint : injectionPointList) {
            injectionPoint.injectTo(instance, this);
        }
        // call matching Injection Listeners for this instance type
        Class<?> instanceType = instance.getClass();
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
    }
}
