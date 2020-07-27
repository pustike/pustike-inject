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

import java.lang.annotation.Annotation;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Scope;

/**
 * A binding builder, which allows to specify the scope of binding.
 * @see jakarta.inject.Scope
 * @see jakarta.inject.Singleton
 */
public interface ScopedBindingBuilder {
    /**
     * Instances of this binding are created in the specified scope which should be already registered to the binder.
     * By default, the Singleton scope is registered to the binder.
     * For ex: <pre>{@code bind(Service.class).to(ServiceImpl.class).in(Singleton.class); }</pre>
     * Or
     * <pre>{@code
     * ThreadScope threadScope = new ThreadScope();
     * binder.bindScope(ThreadScoped.class, threadScope);
     * binder.bind(Service.class).to(ServiceImpl.class).in(ThreadScoped.class);
     * }</pre>
     * @param scopeAnnotation the scope annotation, which should be already registered to the binder
     * @see Binder#bindScope(Class, Scope)
     * @see jakarta.inject.Singleton
     */
    void in(Class<? extends Annotation> scopeAnnotation);

    /**
     * Instances of this binding are created in the specified scope which should be already registered to the binder.
     * For ex:
     * <pre>{@code
     * ThreadScope threadScope = new ThreadScope();
     * binder.bindScope(ThreadScoped.class, threadScope);
     * binder.bind(Service.class).to(ServiceImpl.class).in(threadScope);
     * }</pre>
     * @param scope the scope instance, which should be already registered to the binder
     * @see Binder#bindScope(Class, Scope)
     */
    void in(Scope scope);

    /**
     * Instructs the {@link Injector} to eagerly initialize this singleton-scoped binding upon creation.
     * For ex: <pre>{@code binder.bind(Service.class).to(ServiceImpl.class).asEagerSingleton(); }</pre>
     */
    void asEagerSingleton();

    /**
     * Instructs the {@link Injector} to lazily initialize (only when requested), this singleton-scoped binding.
     * For ex: <pre>{@code binder.bind(Service.class).to(ServiceImpl.class).asLazySingleton(); }</pre>
     * @see jakarta.inject.Singleton
     */
    void asLazySingleton();
}
