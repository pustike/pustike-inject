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

import java.util.EventObject;

/**
 * Typed event that provides the type of source object used.
 * @param <T> the type of event object being published
 */
public class Event<T> extends EventObject {
    private final Object eventContext;

    /**
     * Constructs a generic Application Event.
     * @param source the object on which the event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public Event(T source) throws IllegalArgumentException {
        this(source, null);
    }

    /**
     * Constructs a generic Application Event with the given context value.
     * @param source       the object on which the event initially occurred.
     * @param eventContext the event context value, can be null
     * @throws IllegalArgumentException if source is null.
     */
    public Event(T source, Object eventContext) throws IllegalArgumentException {
        super(source);
        this.eventContext = eventContext;
    }

    /**
     * The object on which the Event initially occurred.
     * @return The object on which the Event initially occurred.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final T getSource() {
        return (T) source;
    }

    /**
     * Gets the type of event source.
     * @return type of the event source
     */
    public Class<?> getSourceType() {
        return source.getClass();
    }

    /**
     * Gets the event context if available, can be null.
     * @param <V> the type of event context value
     * @return event context if available, can be null
     */
    @SuppressWarnings("unchecked")
    public <V> V getContext() {
        return (V) eventContext;
    }
}
