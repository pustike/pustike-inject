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
package io.github.pustike.inject.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.InjectionPoint;

final class FieldInjectionPoint<T> implements InjectionPoint<T> {
    private final BindingKey<T> targetKey;
    private final Field field;
    // Whether this field supports null values to be injected.
    private final boolean allowNullValue;
    private boolean isStaticFieldInjected;

    FieldInjectionPoint(Field field, BindingKey<T> targetKey, boolean allowNullValue) {
        this.targetKey = targetKey;
        this.field = field;
        this.allowNullValue = allowNullValue;
    }

    @Override
    public Object injectTo(T instance, Injector injector) {
        try {
            if (isStaticFieldInjected) {
                return null; // do not set a static field more than once!
            }
            isStaticFieldInjected = Modifier.isStatic(field.getModifiers());
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            T value = injector.getInstance(targetKey);
            if (value == null && !allowNullValue) {
                throw new NullPointerException("Field: " + field + " doesn't allow null values!");
            }
            field.set(instance, value);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("error when injecting dependency into " + toString(), e);
        }
    }

    @Override
    public String toString() {
        return "field=" + field + " -> " + targetKey;
    }
}