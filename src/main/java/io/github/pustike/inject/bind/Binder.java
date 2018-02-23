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
package io.github.pustike.inject.bind;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;
import io.github.pustike.inject.spi.BindingListener;
import io.github.pustike.inject.spi.InjectionListener;

/**
 * Collects configuration information (primarily bindings) which will be used to create an Injector. The Binder is
 * passed as an argument to modules and each of them contribute their own bindings using the binder.
 */
public interface Binder {
    /**
     * Binds the given type which can be further annotated in the {@link AnnotatedBindingBuilder}.
     * @param instanceType the instance type
     * @param <T>          the type of the class specified in this binding
     * @return the annotated binding builder
     */
    <T> AnnotatedBindingBuilder<T> bind(Class<T> instanceType);

    /**
     * Binds the key which needs to be linked to a target in the {@link LinkedBindingBuilder}.
     * @param key the binding key
     * @param <T> the type of the class specified in this binding
     * @return the linked binding builder
     */
    <T> LinkedBindingBuilder<T> bind(BindingKey<T> key);

    /**
     * Binds the given type as {@link MultiBinder} to which additional bindings can be added.
     * @param instanceType the instance type
     * @param <T>          the type of the class specified in this binding
     * @return the multi binder
     */
    <T> MultiBinder<T> multiBinder(Class<T> instanceType);

    /**
     * Binds the given key as {@link MultiBinder} to which additional bindings can be added.
     * @param key the binding key
     * @param <T> the type of the class specified in this binding
     * @return the multi binder
     */
    <T> MultiBinder<T> multiBinder(BindingKey<T> key);

    /**
     * Uses the given module to configure more bindings.
     * <p>
     * This allows for composition i.e. FooModule may install FooServiceModule (for instance). This would mean that
     * an Injector created based only on FooModule will include bindings and providers in both FooModule and
     * FooServiceModule. But same module can not be installed more than once, as duplicate bindings are not allowed.
     * @param module the module to install
     */
    void install(Module module);

    /**
     * Binds a scope annotation to the given scope instance.
     * For ex:
     * <pre>{@code
     * ThreadScope threadScope = new ThreadScope();
     * binder.bindScope(ThreadScoped.class, threadScope);
     * binder.bind(Service.class).to(ServiceImpl.class).in(ThreadScoped.class);
     * }</pre>
     * This binds the Service class to its implementation in the ThreadScope
     * @param scopeAnnotation the scope annotation type
     * @param scope           the scope instance
     */
    void bindScope(Class<? extends Annotation> scopeAnnotation, Scope scope);

    /**
     * Set the default scope to be applied when not specified for the binding. When not set, 'prototype' or 'per
     * call' scope is used which will create new instances each time injector supplies a value.
     * @param scopeAnnotation the default scope annotation to apply to bindings, if not specified
     */
    void setDefaultScope(Class<? extends Annotation> scopeAnnotation);

    /**
     * Registers an injection listener which will be notified after dependencies are injected into a newly
     * provisioned instance, if target class is matched by the given type matcher.
     * @param typeMatcher       that matches injectable types, the listener should be notified of
     * @param injectionListener the injection listener matched by typeMatcher
     */
    void addInjectionListener(Predicate<Class<?>> typeMatcher, InjectionListener injectionListener);

    /**
     * Registers a binding listener which will be notified during injector configuration, if matched by the given
     * type matcher, after the targetClass binding is registered to the injector.
     * For ex:
     *
     * <pre>{@code
     *  Predicate<Class<?>> predicate = targetType -> targetType.getDeclaredAnnotation(Controller.class) != null;
     *  binder.addBindingListener(predicate, this::registerController);
     * }</pre>
     * The above binding listener matches all target classes having @Controller annotation and when notified
     * further configuration is done using registerController method.
     * @param typeMatcher     that matches injectable types, the listener should be notified of
     * @param bindingListener the binding listener matched by typeMatcher
     */
    void addBindingListener(Predicate<Class<?>> typeMatcher, BindingListener bindingListener);
}
