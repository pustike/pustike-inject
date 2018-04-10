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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.pustike.inject.BindingKey;

final class ObserverRegistry {
    private final Map<Integer, List<Observer>> eventObserversMap;

    ObserverRegistry() {
        eventObserversMap = new HashMap<>();
    }

    /**
     * Find all methods in the given class and all its super-classes, that are annotated with {@code @Observes}.
     * @param targetClass the target listener class
     * @return a list of observer methods
     */
    private static Iterable<Method> findObserverMethods(Class<?> targetClass) {
        Map<Integer, Method> observerMethods = new HashMap<>();
        for (Class<?> clazz = targetClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Observes.class) && !method.isSynthetic()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != 1) {
                        String message = "Method %s has @Observes annotation but has %d parameters."
                                + " EventHandler methods must have exactly 1 parameter.";
                        throw new IllegalArgumentException(String.format(message, method, parameterTypes.length));
                    }
                    int hashCode = Objects.hash(method.getName(), parameterTypes);
                    if (!observerMethods.containsKey(hashCode)) {
                        observerMethods.put(hashCode, method);
                    }
                }
            }
        }
        return Collections.unmodifiableCollection(observerMethods.values());
    }

    private static int computeParameterHashCode(Method method) {
        Class<?> parameterClass = method.getParameterTypes()[0];
        Type parameterType = method.getGenericParameterTypes()[0];
        if (parameterType instanceof ParameterizedType) {
            ParameterizedType firstParam = (ParameterizedType) parameterType;
            Type[] typeArguments = firstParam.getActualTypeArguments();
            return Objects.hash(firstParam.getRawType().getTypeName(), typeArguments[0].getTypeName());
        }
        return parameterClass.getName().hashCode();
    }

    private static int computeEventHashCode(Object eventObject) {
        Class<?> eventClass = eventObject.getClass();
        if (eventObject instanceof Event) {
            return Objects.hash(eventClass.getName(), ((Event) eventObject).getSourceType().getName());
        }// REVIEW: doesn't support complex generic events!
        return eventClass.getName().hashCode();
    }

    void register(BindingKey<?> bindingKey, Class<?> targetClass) {
        Iterable<Method> observerMethods = findObserverMethods(targetClass);
        for (Method method : observerMethods) {
            int eventHashCode = computeParameterHashCode(method);
            List<Observer> observerList = eventObserversMap.get(eventHashCode);
            if (observerList == null) {
                observerList = new ArrayList<>();
                eventObserversMap.put(eventHashCode, observerList);
            }
            observerList.add(new Observer(bindingKey, method));
        }
    }

    // REVIEW: should it support event hierarchy?
    Iterator<Observer> findObservers(Object eventObject) {
        int eventHashCode = computeEventHashCode(eventObject);
        List<Observer> observers = eventObserversMap.get(eventHashCode);
        return observers == null ? Collections.emptyIterator()
                : Collections.unmodifiableCollection(observers).iterator();
    }

    /**
     * Discards all entries in the cache.
     */
    void invalidateAll() {
        eventObserversMap.clear();
    }
}
