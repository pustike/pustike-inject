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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * Handler for dispatching events to observers, providing different event ordering guarantees that make sense for
 * different situations.
 */
class Dispatcher {
    // Per-thread queue of events to dispatch.
    private static final ThreadLocal<Queue<EventData>> queue = ThreadLocal.withInitial(ArrayDeque::new);
    // Per-thread dispatch state, used to avoid reentrant event dispatching.
    private static final ThreadLocal<Boolean> dispatching = ThreadLocal.withInitial(() -> false);
    // the eventBus to invoke the event handler
    private final EventBus eventBus;

    Dispatcher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Dispatches the given {@code event} to all {@code observers} using perThreadDispatchQueue.
     *
     * <p>It queues events that are posted reentrantly on a thread that is already dispatching an event,
     * guaranteeing that all events posted on a single thread are dispatched to all observers in the order they
     * are posted.
     *
     * <p>When all observers are dispatched to using a <i>direct</i> executor (which dispatches on the same thread
     * that posts the event), this yields a breadth-first dispatch order on each thread. That is, all observers of a
     * single event A will be called before any observers of any events B and C that are posted to the event bus by
     * the observers to A.
     */
    void dispatch(Object event, Iterator<Observer> observers) {
        Queue<EventData> queueForThread = queue.get();
        queueForThread.offer(new EventData(event, observers));

        if (!dispatching.get()) {
            dispatching.set(true);
            try {
                EventData nextEvent;
                while ((nextEvent = queueForThread.poll()) != null) {
                    while (nextEvent.observers.hasNext()) {
                        eventBus.invokeObserverMethod(nextEvent.event, nextEvent.observers.next());
                    }
                }
            } finally {
                dispatching.remove();
                queue.remove();
            }
        }
    }

    private static final class EventData {
        private final Object event;
        private final Iterator<Observer> observers;

        private EventData(Object event, Iterator<Observer> observers) {
            this.event = event;
            this.observers = observers;
        }
    }
}
