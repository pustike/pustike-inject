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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
import io.github.pustike.inject.bind.LinkedBindingBuilder;
import io.github.pustike.inject.bind.Module;
import io.github.pustike.inject.bind.MultiBinder;
import io.github.pustike.inject.bind.Provides;
import io.github.pustike.inject.spi.BindingListener;
import io.github.pustike.inject.spi.InjectionListener;

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

    void configure(Iterable<Module> modules) {
        modules.forEach(this::configureModule);
        bindingBuilderList.forEach(bindingBuilder -> bindingBuilder.build(injector));
    }

    private void configureModule(Module module) {
        defaultScope = getScope(Scopes.PER_CALL);
        module.configure(this);
        configureProvidesBindings(module);
    }

    @Override
    public <T> AnnotatedBindingBuilder<T> bind(Class<T> instanceType) {
        return addNewBindingBuilder(BindingKey.of(instanceType), false);
    }

    @Override
    public <T> LinkedBindingBuilder<T> bind(BindingKey<T> key) {
        return addNewBindingBuilder(key, false);
    }

    @Override
    public <T> MultiBinder<T> multiBinder(Class<T> instanceType) {
        return addNewBindingBuilder(BindingKey.of(instanceType), true);
    }

    @Override
    public <T> MultiBinder<T> multiBinder(BindingKey<T> key) {
        return addNewBindingBuilder(key, true);
    }

    private <T> DefaultBindingBuilder<T> addNewBindingBuilder(BindingKey<T> key, boolean multiBinder) {
        DefaultBindingBuilder<T> builder = new DefaultBindingBuilder<>(key, this, defaultScope, multiBinder);
        bindingBuilderList.add(builder);
        return builder;
    }

    @Override
    public void install(Module module) {
        Objects.requireNonNull(module);
        Scope currentScope = defaultScope;
        configureModule(module);
        defaultScope = currentScope;
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

    Scope getScope(Annotation[] annotations, Scope defaultScope) {
        Class<? extends Annotation> scopeAnnotation = getScopeAnnotation(annotations);
        return scopeAnnotation != null ? getScope(scopeAnnotation.getName()) : defaultScope;
    }

    private static Class<? extends Annotation> getScopeAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(javax.inject.Scope.class)) {
                return annotation.annotationType();
            }
        }
        return null;
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

    void visitTypeBindingListeners(BindingKey<?> bindingKey, Class<?> instanceType) {
        if (!bindingListenerMatcherMap.isEmpty()) {
            for (Map.Entry<BindingListener, Predicate<Class<?>>> mapEntry : bindingListenerMatcherMap.entrySet()) {
                if (mapEntry.getValue().test(instanceType)) {
                    mapEntry.getKey().afterBinding(bindingKey, instanceType);
                }
            }
        }
    }

    private void configureProvidesBindings(Module module) {
        for (Class<?> c = module.getClass(); c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Provides.class)) {
                    addProvidesBinding(module, method);
                }
            }
        }
    }

    private void addProvidesBinding(Module module, Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType == void.class) {
            throw new RuntimeException("@Provides method should not have 'void' as return type : " + method);
        }
        Annotation[] annotations = method.getAnnotations();
        BindingKey<?> bindingKey = new InjectionTarget<>(returnType, annotations).getKey();
        bind(bindingKey).toProvider(InstanceProvider.from(method, module)).in(getScope(annotations, defaultScope));
    }

    void clear() {
        bindingBuilderList.clear();
        annotationScopeMap.clear();
        bindingListenerMatcherMap.clear();
    }
}
