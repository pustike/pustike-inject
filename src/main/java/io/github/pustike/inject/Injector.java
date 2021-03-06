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
package io.github.pustike.inject;

import java.util.Optional;
import jakarta.inject.Provider;

import io.github.pustike.inject.bind.Module;

/**
 * The injector is used to create objects with all their dependencies (fields and constructor/methods) injected.
 * Injector is configured with bindings specified by {@link Module modules}. These bindings are scanned to identify
 * {@link io.github.pustike.inject.spi.InjectionPoint}s and are registered with the binding key. When an instance of
 * a type or a binding key is requested, injector returns the instance by creating it and injecting all its
 * declared dependencies.
 * <p>
 * Following bindings are present automatically:<ul>
 * <li>The {@code Injector} itself.
 * <li>A {@code Provider<T>} for each binding of type {@code T}
 * </ul>
 * To create an {@code injector}, use {@code create} factory methods of {@link Injectors} class.
 */
public interface Injector {
    /**
     * Returns an instance of {@code instanceType}, if a matching binding is present, after injecting all
     * dependencies into its constructor, fields and methods.
     * @param type the requested type.
     * @param <T>  the type of instance
     * @return the created instance with all dependencies injected into fields and methods/constructor.
     * @throws NoSuchBindingException no matching binding has been registered with the injector
     * @see #getInstance(BindingKey)
     */
    <T> T getInstance(Class<T> type) throws NoSuchBindingException;

    /**
     * Returns an instance of the binding that has been registered for the given key, after injecting all
     * dependencies into its constructor, fields and methods.
     * @param key A binding key, for which a binding has been registered.
     * @param <T> the type of instance
     * @return the created instance with all dependencies injected into fields and methods/constructor.
     * @throws NoSuchBindingException no matching binding has been registered with the injector
     * @see #getInstance(Class)
     */
    <T> T getInstance(BindingKey<T> key) throws NoSuchBindingException;

    /**
     * Returns an {@link Optional} instance of the given {@code type}, if a matching binding is present,
     * after injecting all dependencies into its constructor, fields and methods.
     * @param type the requested type.
     * @param <T>  the type of instance
     * @return the optional instance for the given type, if binding is present.
     * @see #getInstance(Class)
     */
    <T> Optional<T> getIfPresent(Class<T> type);

    /**
     * Returns an {@link Optional} instance for the given {@code key}, if a matching binding is present,
     * after injecting all dependencies into its constructor, fields and methods.
     * @param key  the target binding key
     * @param <T>  the type of instance
     * @return the optional instance for the given key, if binding is present.
     * @see #getInstance(BindingKey)
     */
    <T> Optional<T> getIfPresent(BindingKey<T> key);

    /**
     * Returns the instance provider of the type, if a matching binding is present.
     * @param type the type of the requested instance provider.
     * @param <T>  the type of instance
     * @return the instance provider for the type
     * @throws NoSuchBindingException no matching binding has been registered with the injector
     * @see #getInstance(BindingKey)
     */
    <T> Provider<T> getProvider(Class<T> type) throws NoSuchBindingException;

    /**
     * Returns the instance provider of the type that has been registered for the given key.
     * @param key A binding key, for which a binding has been registered.
     * @param <T> the type of instance
     * @return the instance provider for the key
     * @throws NoSuchBindingException no matching binding has been registered with the injector
     * @see #getInstance(Class)
     */
    <T> Provider<T> getProvider(BindingKey<T> key) throws NoSuchBindingException;

    /**
     * Injects members into the given instance, as if it where created by the injector itself.
     * In other words, fills fields and invokes methods annotated with @Inject, assuming that a binding is present for
     * those fields, and method parameters.
     * @param instance the instance to which members need to be injected
     * @throws NoSuchBindingException if any of the declared dependencies is not bound and is not optional
     */
    void injectMembers(Object instance) throws NoSuchBindingException;

    /**
     * Returns this injector's parent, or {@code null} if this is a top-level injector.
     * @return the parent injector if present, else {@code null}
     * @see #createChildInjector(Module...)
     */
    Injector getParent();

    /**
     * Returns a new injector that delegates all requests for bindings that are not found, to its parent injector.
     * All bindings in the parent injector are visible to the child, but elements of the child injector are
     * not visible to its parent.
     * @param modules an array of modules specifying type bindings
     * @return the newly created child injector
     * @see #createChildInjector(Iterable)
     * @see #getParent()
     */
    Injector createChildInjector(Module... modules);

    /**
     * Returns a new injector that delegates all requests for bindings that are not found, to its parent injector.
     * All bindings in the parent injector are visible to the child, but elements of the child injector are
     * not visible to its parent.
     * @param modules an iterable (list) of modules specifying type bindings
     * @return the newly created child injector
     * @see #createChildInjector(Module...)
     * @see #getParent()
     */
    Injector createChildInjector(Iterable<Module> modules);
}
