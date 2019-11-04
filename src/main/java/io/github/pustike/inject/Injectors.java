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

import java.util.List;

import io.github.pustike.inject.bind.Module;
import io.github.pustike.inject.impl.DefaultInjector;
import io.github.pustike.inject.spi.InjectionPointLoader;

/**
 * This class provides factory methods to create {@link Injector} with bindings specified by {@link Module modules}
 * and using an {@link InjectionPointLoader}.
 */
public final class Injectors {
    /**
     * Creates a new {@link Injector} with bindings specified by given modules.
     * @param modules an array of modules specifying type bindings
     * @return the newly created injector
     * @see #create(Iterable)
     */
    public static Injector create(Module... modules) {
        return DefaultInjector.create(null, List.of(modules));
    }

    /**
     * Creates a new {@link Injector} with bindings specified by given modules.
     * @param modules an iterable (list) of modules specifying type bindings
     * @return the newly created injector
     * @see #create(InjectionPointLoader, Iterable)
     */
    public static Injector create(Iterable<Module> modules) {
        return DefaultInjector.create(null, modules);
    }

    /**
     * Creates a new {@link Injector} with bindings specified by given modules and using an
     * {@link InjectionPointLoader injection point loader}.
     * @param injectionPointLoader the injection point loader,
     * @param modules              an iterable (list) of modules specifying type bindings
     * @return the newly created injector
     * @see #create(Module...)
     */
    public static Injector create(InjectionPointLoader injectionPointLoader, Iterable<Module> modules) {
        return DefaultInjector.create(injectionPointLoader, modules);
    }

    /**
     * Dispose or release all data held by the Default Injector.
     * @param injector the default injector
     * @throws IllegalArgumentException if injector is not an instance of Default Injector
     */
    public static void dispose(Injector injector) {
        if (injector instanceof DefaultInjector) {
            ((DefaultInjector) injector).dispose();
        } else {
            throw new IllegalArgumentException("Disposing is supported only for default injector!");
        }
    }

    private Injectors() {
    }
}
