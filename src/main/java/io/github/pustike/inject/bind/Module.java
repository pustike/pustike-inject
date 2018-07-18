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
package io.github.pustike.inject.bind;

import io.github.pustike.inject.Injector;

/**
 * A module contributes configuration information, typically interface bindings, which will be used to create an
 * {@link Injector}. For ex:
 * <pre>{@code
 * Module module = binder -> {
 *      binder.bind(Service.class).to(ServiceImpl.class).in(Singleton.class);
 *      ...
 * };
 * Injector injector = Injectors.create(module);
 * Service service = injector.getInstance(Service.class);
 * }</pre>
 */
@FunctionalInterface
public interface Module {
    /**
     * Called to create bindings, by using the given {@link Binder binder} during injector creation.
     * @param binder the binder
     */
    void configure(Binder binder);
}
