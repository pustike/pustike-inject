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
package io.github.pustike.inject.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.NoSuchBindingException;
import io.github.pustike.inject.spi.InjectionPoint;

final class FieldInjectionPoint<T> implements InjectionPoint<T> {
    private final InjectionTarget<T> injectionTarget;
    private final Field field;
    private boolean isStaticFieldInjected;

    FieldInjectionPoint(Field field) {
        this.field = field;
        this.injectionTarget = new InjectionTarget<>(field.getGenericType(), field.getAnnotations());
        if (!field.trySetAccessible()) {
            throw new InaccessibleObjectException("couldn't enable access to " + toString());
        }
    }

    @Override
    public Object injectTo(T instance, Injector injector) throws NoSuchBindingException {
        if (isStaticFieldInjected) {
            return null; // do not set a static field more than once!
        }
        try {
            field.set(instance, injectionTarget.getValue(injector));
            isStaticFieldInjected = Modifier.isStatic(field.getModifiers());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("error when injecting dependency into " + toString(), e);
        }
        return instance;
    }

    @Override
    public String toString() {
        return "field:" + System.lineSeparator() + field.getDeclaringClass().getTypeName() + '.' + field.getName()
                + " -> " + injectionTarget.getBindingKey();
    }
}
