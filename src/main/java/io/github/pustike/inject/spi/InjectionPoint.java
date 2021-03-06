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
package io.github.pustike.inject.spi;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.NoSuchBindingException;

/**
 * Injector uses injection points to inject values into fields and methods/constructor.
 * @param <T> the type of the instance
 */
@FunctionalInterface
public interface InjectionPoint<T> {
    /**
     * Inject dependencies into the instance at the injection point using the given injector.
     * @param instance the instance to inject into, can be null for method/constructor injection
     * @param injector the default dependency injector
     * @return the result of the injection point invocation, may be null
     * @throws NoSuchBindingException if any of the declared dependencies is not bound and is not optional
     */
    Object injectTo(T instance, Injector injector) throws NoSuchBindingException;
}
