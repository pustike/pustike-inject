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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.inject.Provider;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Names;
import io.github.pustike.inject.Scope;
import io.github.pustike.inject.bind.AnnotatedBindingBuilder;
import io.github.pustike.inject.bind.LinkedBindingBuilder;
import io.github.pustike.inject.bind.MultiBinder;
import io.github.pustike.inject.bind.ScopedBindingBuilder;

/**
 * Default implementation of a binding builder. Implements {@link AnnotatedBindingBuilder},
 * thus {@link ScopedBindingBuilder}, and {@link LinkedBindingBuilder} as well. In other words:
 * Under the hood, you are always using this one and only binding builder.
 */
final class DefaultBindingBuilder<T> implements AnnotatedBindingBuilder<T>, MultiBinder<T> {
    private static final String SCOPE_METHOD_LIST = "toInstance(Object), scope(Scopes)," +
            " asEagerSingleton(), and asLazySingleton()";
    private static final String TARGET_METHOD_LIST = "toInstance(Object), to(Class),"
            + " to(Constructor), to(Method), to(Provider, Class)";
    private final BindingKey<T> sourceKey;
    private final DefaultBinder binder;
    private final Scope defaultScope;
    private Annotation sourceAnnotation;
    private Class<? extends Annotation> sourceAnnotationType;
    private Class<? extends T> targetType;
    private Provider<? extends T> targetProvider;
    private Scope scope;
    // for multi-binding
    private final boolean multiBinder;
    private final List<Binding<T>> bindingList;
    private boolean addingBinding;

    DefaultBindingBuilder(BindingKey<T> key, DefaultBinder binder, Scope defaultScope, boolean multiBinder) {
        this.sourceKey = key;
        this.binder = binder;
        this.defaultScope = defaultScope;
        this.multiBinder = multiBinder;
        this.bindingList = new ArrayList<>();
    }

    @Override
    public void toInstance(T instance) {
        if (instance == null) {
            throw new NullPointerException("The target instance must not be null.");
        }
        checkNoTarget();
        @SuppressWarnings("unchecked")
        Class<? extends T> instanceClass = (Class<? extends T>) instance.getClass();
        targetType = instanceClass;
        targetProvider = () -> instance;
        asEagerSingleton();
    }

    @Override
    public ScopedBindingBuilder to(Class<? extends T> implementation) {
        checkNoTarget();
        targetType = Objects.requireNonNull(implementation);
        return this;
    }

    @Override
    public ScopedBindingBuilder to(Constructor<? extends T> constructor) {
        if (constructor == null) {
            throw new NullPointerException("The target constructor must not be null.");
        }
        checkNoTarget();
        targetProvider = InstanceProvider.from(constructor);
        return this;
    }

    @Override
    public ScopedBindingBuilder to(Method factoryMethod) {
        if (factoryMethod == null) {
            throw new NullPointerException("The target constructor must not be null.");
        } else if (!Modifier.isStatic(factoryMethod.getModifiers())) {
            throw new IllegalStateException("The target method must be static.");
        } else if (factoryMethod.getReturnType().isPrimitive()) {
            throw new IllegalStateException("The target method must return a non-primitive result.");
        } else if (factoryMethod.getReturnType().isArray()) {
            throw new IllegalStateException("The target method must return a single object, and not an array.");
        } else if (Void.TYPE == factoryMethod.getReturnType()) {
            throw new IllegalStateException("The target method must return a non-void result.");
        }
        checkNoTarget();
        targetProvider = InstanceProvider.from(factoryMethod, null);
        return this;
    }

    @Override
    public ScopedBindingBuilder toProvider(Provider<? extends T> provider) {
        checkNoTarget();
        targetProvider = Objects.requireNonNull(provider);
        targetType = sourceKey.getType();
        return this;
    }

    @Override
    public ScopedBindingBuilder toProvider(Class<? extends Provider<? extends T>> providerType) {
        checkNoTarget();
        Objects.requireNonNull(providerType);
        targetProvider = InstanceProvider.fromProvider(providerType);
        targetType = sourceKey.getType();
        return this;
    }

    @Override
    public void in(Class<? extends Annotation> scopeAnnotation) {
        in(binder.getScope(scopeAnnotation.getName()));
    }

    @Override
    public void in(Scope scope) {
        if (this.scope != null) {
            throw new IllegalStateException("The methods " + SCOPE_METHOD_LIST
                    + " are mutually exclusive, and may be invoked only once.");
        }
        this.scope = Objects.requireNonNull(scope);
    }

    @Override
    public void asEagerSingleton() {
        in(binder.getScope(Scopes.EAGER_SINGLETON));
    }

    @Override
    public void asLazySingleton() {
        in(binder.getScope(Scopes.SINGLETON));
    }

    @Override
    public LinkedBindingBuilder<T> named(String name) {
        return annotatedWith(Names.named(name));
    }

    @Override
    public LinkedBindingBuilder<T> annotatedWith(Annotation annotation) {
        if (sourceAnnotation != null) {
            throw new IllegalStateException("The method annotatedWith(Annotation) must not be invoked twice.");
        }
        sourceAnnotation = Objects.requireNonNull(annotation);
        return this;
    }

    @Override
    public LinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType) {
        if (sourceAnnotationType != null) {
            throw new IllegalStateException("The method annotatedWith(Class) must not be invoked twice.");
        }
        sourceAnnotationType = Objects.requireNonNull(annotationType);
        return this;
    }

    @Override
    public LinkedBindingBuilder<T> addBinding() {
        if (addingBinding) {
            doAddBinding();
        }
        this.addingBinding = true;
        return this;
    }

    private void doAddBinding() {
        if (targetProvider == null && targetType == null) {
            throw new IllegalStateException("The target instance or a provider should be configured!");
        }
        bindingList.add(new Binding<>(sourceKey, getInstanceProvider(), getScope()));
        targetType = null;
        targetProvider = null;
        scope = null;
    }

    @SuppressWarnings("unchecked")
    void build(DefaultInjector injector) {
        if (addingBinding) {
            doAddBinding();
        }
        BindingKey<T> bindingKey = sourceAnnotation != null ? BindingKey.of(sourceKey.getType(), sourceAnnotation)
                : BindingKey.of(sourceKey.getType(), sourceAnnotationType);
        bindingKey = multiBinder ? (BindingKey<T>) bindingKey.toListType() : bindingKey;
        Binding<T> binding = multiBinder ? new Binding<>(bindingKey, bindingList, getScope(), injector)
                : new Binding<>(bindingKey, getInstanceProvider(), getScope(), injector);
        injector.register(bindingKey, binding);
        // call matching TypeBindingListeners for this binding targetType
        Class<? extends T> instanceType = targetType == null ? sourceKey.getType() : targetType;
        binder.visitTypeBindingListeners(bindingKey, instanceType);
    }

    private Scope getScope() {
        if (scope == null) {
            Class<?> instanceType = targetType != null ? targetType : sourceKey.getType();
            scope = binder.getScope(instanceType.getDeclaredAnnotations(), defaultScope);
        }
        if (scope == null) {
            throw new IllegalStateException("Neither of the methods " + SCOPE_METHOD_LIST
                    + " has been invoked on this binding builder.");
        }
        return scope;
    }

    @SuppressWarnings("unchecked")
    private Provider<T> getInstanceProvider() {
        if (targetProvider != null) {
            return (Provider<T>) targetProvider;
        } else if (targetType != null) {
            return InstanceProvider.from(targetType);
        }
        Class<T> sourceType = sourceKey.getType();
        if (sourceType != null) {
            if (sourceType.isInterface() || Modifier.isAbstract(sourceType.getModifiers())) {
                throw new IllegalStateException("Neither of the methods " + TARGET_METHOD_LIST
                        + " have been invoked on this binding builder, but cannot bind " + sourceType.getName()
                        + " as target type, because it is an interface, or an abstract class.");
            }
            return InstanceProvider.from(sourceType);
        }
        throw new IllegalStateException("Neither of the methods " + TARGET_METHOD_LIST
                + " have been invoked on this binding builder.");
    }

    private void checkNoTarget() {
        if (targetType != null || targetProvider != null) {
            throw new IllegalStateException("The methods " + TARGET_METHOD_LIST
                    + " are mutually exclusive, and may be invoked only once.");
        }
    }
}
