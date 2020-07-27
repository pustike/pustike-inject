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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import jakarta.inject.Provider;

/**
 * A binding builder, which allows to specify a bindings target: A target is the value, that gets injected, if the
 * binding is applied.
 * @param <T> the type of the class specified in this binding
 */
public interface LinkedBindingBuilder<T> extends ScopedBindingBuilder {
    /**
     * Specifies the binding target to be the specified instance.
     * For ex:
     * <pre>{@code binder.bind(Service.class).to(new ServiceImpl()); }</pre>
     * @param instance the target instance to bind
     */
    void toInstance(T instance);

    /**
     * Binds the interface to the implementation as the target which is provisioned by the injector.
     * For ex:
     * <pre>{@code binder.bind(Service.class).to(ServiceImpl.class); }</pre>
     * @param implementation the implementing class
     * @return the scoped binding builder
     */
    ScopedBindingBuilder to(Class<? extends T> implementation);

    /**
     * Binds the interface to constructor of the implementation which is used create new instances by the injector.
     * For ex:
     * <pre>{@code
     *  Constructor<?> loneConstructor = ServiceImpl.class.getDeclaredConstructors()[0];
     *  binder.bind(Service.class).toConstructor(loneConstructor);
     * }</pre>
     *
     * Specifies which constructor to use in a concrete class implementation. It means that @Inject need not be
     * placed on any of the constructors and that Injector treats the provided constructor as though it were
     * annotated so. It is useful for cases where existing classes cannot be modified and it is a bit simpler than
     * using a Provider.
     * @param constructor the constructor of the implementation
     * @return the scoped binding builder
     */
    ScopedBindingBuilder to(Constructor<? extends T> constructor);

    /**
     * Binds the interface to the factory method which is used create new instances by the injector.
     * For ex:
     * <pre>{@code
     *  Method factoryMethod = ServiceImpl.class.getMethod("create");
     *  binder.bind(Service.class).to(factoryMethod);
     * }</pre>
     *
     * It is useful for cases where existing classes cannot be modified and it is a bit simpler than using a Provider.
     * @param factoryMethod the factory method providing the target instance
     * @return the scoped binding builder
     */
    ScopedBindingBuilder to(Method factoryMethod);

    /**
     * Binds the interface to a provider instance which provides instances of the target.
     * For ex:
     * <pre>{@code binder.bind(Service.class).toProvider(new ServiceProvider()); }</pre>
     * @param provider the provider of target instance
     * @return the scoped binding builder
     */
    ScopedBindingBuilder toProvider(Provider<? extends T> provider);

    /**
     * Binds the interface to a provider type which is provisioned by the injector.
     * For ex:
     * <pre>{@code binder.bind(Service.class).toProvider(ServiceProvider.class); }</pre>
     * @param providerType the provider type of target instance
     * @return the scoped binding builder
     */
    ScopedBindingBuilder toProvider(Class<? extends Provider<? extends T>> providerType);
}
