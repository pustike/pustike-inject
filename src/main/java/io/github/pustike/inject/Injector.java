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
package io.github.pustike.inject;

import javax.inject.Provider;

import io.github.pustike.inject.bind.Module;

/**
 * The injector is used to create objects with all their dependencies (fields and constructor/methods) injected.
 * Injector is configured with bindings specified by {@link Module modules}. These bindings are scanned to identify
 * {@link io.github.pustike.inject.bind.InjectionPoint}s and are registered with the binding key. When an instance of
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
     * dependencies  into its fields and methods/constructor.
     * @param type the requested type.
     * @param <T>  the type of instance
     * @return the created instance with all dependencies injected into fields and methods/constructor.
     * @throws NoSuchBindingException no matching binding has been registered with the injector
     * @see #getInstance(BindingKey)
     */
    <T> T getInstance(Class<T> type) throws NoSuchBindingException;

    /**
     * Returns an instance of the binding that has been registered for the given key, after injecting all
     * dependencies  into its fields and methods/constructor.
     * @param key A binding key, for which a binding has been registered.
     * @param <T> the type of instance
     * @return the created instance with all dependencies injected into fields and methods/constructor.
     * @throws NoSuchBindingException no matching binding has been registered with the injector
     * @see #getInstance(Class)
     */
    <T> T getInstance(BindingKey<T> key) throws NoSuchBindingException;

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
     * Injects members into the given instance, as if it where created by the {@link Injector injector} itself.
     * In other words, fills fields and invokes methods annotated with @Inject, assuming that a binding is present for
     * those fields, and method parameters.
     * @param instance the instance to which members need to be injected
     */
    void injectMembers(Object instance);
}
