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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.inject.Singleton;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;
import io.github.pustike.inject.bind.AnnotatedBindingBuilder;
import io.github.pustike.inject.bind.Binder;
import io.github.pustike.inject.bind.BindingListener;
import io.github.pustike.inject.bind.InjectionListener;
import io.github.pustike.inject.bind.LinkedBindingBuilder;
import io.github.pustike.inject.bind.Module;

/**
 * Default implementation of the {@link Binder binder}.
 */
final class DefaultBinder implements Binder {
    private final DefaultInjector injector;
    private final List<DefaultBindingBuilder<?>> bindingBuilderList;
    private final Map<String, Scope> annotationScopeMap;
    private final Map<BindingListener, Predicate<Class<?>>> bindingListenerMatcherMap;
    private Scope defaultScope;

    DefaultBinder(DefaultInjector injector) {
        this.injector = injector;
        bindingBuilderList = new ArrayList<>();
        annotationScopeMap = new ConcurrentHashMap<>();
        bindingListenerMatcherMap = new LinkedHashMap<>();
        defaultScope = Scopes.createPerCallScope();
        // reject binding default scopes to something else, by registering them first!
        annotationScopeMap.put(Scopes.PER_CALL, defaultScope);
        annotationScopeMap.put(Scopes.EAGER_SINGLETON, Scopes.createSingletonScope());
        annotationScopeMap.put(Singleton.class.getName(), Scopes.createSingletonScope());
    }

    DefaultBinder configure(Iterable<Module> modules) {
        for (Module module : modules) {
            defaultScope = annotationScopeMap.get(Scopes.PER_CALL);
            module.configure(this);
        }
        for (DefaultBindingBuilder<?> bindingBuilder : bindingBuilderList) {
            bindingBuilder.build(injector);
        }
        return this;
    }

    @Override
    public <T> AnnotatedBindingBuilder<T> bind(Class<T> instanceType) {
        DefaultBindingBuilder<T> builder = new DefaultBindingBuilder<>(instanceType);
        builder.setDefaultScope(defaultScope);
        builder.setBinder(this);
        bindingBuilderList.add(builder);
        return builder;
    }

    @Override
    public <T> LinkedBindingBuilder<T> bind(BindingKey<T> key) {
        DefaultBindingBuilder<T> builder = new DefaultBindingBuilder<>(key);
        builder.setDefaultScope(defaultScope);
        builder.setBinder(this);
        bindingBuilderList.add(builder);
        return builder;
    }

    @Override
    public void bindScope(Class<? extends Annotation> scopeAnnotation, Scope scope) {
        Scope currentValue = annotationScopeMap.putIfAbsent(scopeAnnotation.getName(), scope);
        if (currentValue != null) {
            throw new IllegalStateException("AnnotationType '" + scopeAnnotation.getName()
                    + "' can be bound only to a single scope!");
        }
    }

    @Override
    public void setDefaultScope(Class<? extends Annotation> scopeAnnotation) {
        // null implies PER_CALL scope here, so that the default scope can be reset to original!
        this.defaultScope = getScope(scopeAnnotation == null ? Scopes.PER_CALL : scopeAnnotation.getName());
    }

    Scope getScope(String scopeName) {
        Scope scope = annotationScopeMap.get(scopeName);
        if (scope == null) {
            throw new IllegalStateException("AnnotationType '" + scopeName + "' is not bound to any scope!");
        }
        return scope;
    }

    @Override
    public void addInjectionListener(Predicate<Class<?>> typeMatcher, InjectionListener injectionListener) {
        Objects.requireNonNull(typeMatcher);
        Objects.requireNonNull(injectionListener);
        injector.bindInjectionListener(typeMatcher, injectionListener);
    }

    @Override
    public void addBindingListener(Predicate<Class<?>> typeMatcher, BindingListener bindingListener) {
        Objects.requireNonNull(typeMatcher);
        Objects.requireNonNull(bindingListener);
        bindingListenerMatcherMap.put(bindingListener, typeMatcher);
    }

    void visitTypeBindingListeners(Class<?> instanceType) {
        Map<BindingListener, Predicate<Class<?>>> typeBindingListenerMap = bindingListenerMatcherMap;
        if (!typeBindingListenerMap.isEmpty()) {
            for (Map.Entry<BindingListener, Predicate<Class<?>>> mapEntry : typeBindingListenerMap.entrySet()) {
                if (mapEntry.getValue().test(instanceType)) {
                    mapEntry.getKey().afterBinding(instanceType);
                }
            }
        }
    }

    void clear() {
        bindingBuilderList.clear();
        annotationScopeMap.clear();
        bindingListenerMatcherMap.clear();
    }
}
