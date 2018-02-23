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
package io.github.pustike.inject.events;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Objects;
import javax.inject.Inject;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.Module;

/**
 * Event Bus allows components, managed by the injector, to interact in a completely decoupled fashion, with no
 * compile-time dependency between them.
 *
 * <p>To be able to publish and observe events, include the {@link #createModule() EventBusModule} when configuring
 * the injector. For ex:
 * <pre>{@code Injector injector = Injectors.create(EventBus.createModule(), otherModules); }</pre>
 *
 * An instance of the EventBus can be then obtained from Injector or by declaring it as a dependency to be injected.
 */
public class EventBus {
    private final Injector injector;
    private final ObserverRegistry registry;
    private final Dispatcher dispatcher;

    /**
     * Constructs EventBus with given injector and registry.
     * @param injector the injector
     * @param registry the observer registry
     */
    @Inject
    EventBus(Injector injector, ObserverRegistry registry) {
        this.injector = Objects.requireNonNull(injector);
        this.registry = Objects.requireNonNull(registry);
        this.dispatcher = new Dispatcher(this);
    }

    /**
     * Create a new {@link Module} which binds the EventBus in {@link javax.inject.Singleton} scope. And adds a
     * {@link io.github.pustike.inject.spi.BindingListener} which visits on all types to find and register methods
     * annotated with {@link Observes}.
     * @return the new event bus module
     */
    public static Module createModule() {
        return binder -> {
            binder.bind(EventBus.class).asLazySingleton();
            ObserverRegistry registry = new ObserverRegistry();
            binder.bind(ObserverRegistry.class).toInstance(registry);
            binder.addBindingListener(targetClass -> true, registry::register);
        };
    }

    /**
     * Publish the event to all registered observers.
     * @param event event to post.
     */
    public void publish(Object event) {
        Objects.requireNonNull(event);
        Iterator<Observer> eventObservers = registry.findObservers(event);
        if (eventObservers.hasNext()) {
            dispatcher.dispatch(event, eventObservers);
        }
    }

    void invokeObserverMethod(Object event, Observer observer) {
        try {
            Object instance = injector.getInstance(observer.getBindingKey());
            observer.getMethod().invoke(instance, event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clear all observers from the cache.
     */
    public void close() {
        registry.invalidateAll();
    }
}
