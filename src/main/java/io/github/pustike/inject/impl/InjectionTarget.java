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

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import javax.inject.Qualifier;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Injector;

final class InjectionTarget<T> {
    private final BindingKey<T> bindingKey;
    private final boolean nullable;
    private boolean optionalType;

    InjectionTarget(Type genericType, Annotation[] annotations) {
        this.bindingKey = createBindingKey(genericType, annotations);
        this.nullable = allowsNullValue(annotations);
    }

    BindingKey<T> getKey() {
        return bindingKey;
    }

    Object getValue(Injector injector) {
        return optionalType ? injector.getIfPresent(bindingKey) :
                nullable ? injector.getIfPresent(bindingKey).orElse(null) :
                        injector.getInstance(bindingKey);
    }

    static InjectionTarget<?>[] createParameterTargets(Executable executable) {
        Type[] parameterTypes = executable.getGenericParameterTypes();
        Annotation[][] annotations = executable.getParameterAnnotations();
        InjectionTarget<?>[] injectionTargets = new InjectionTarget[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            injectionTargets[i] = new InjectionTarget<>(parameterTypes[i], annotations[i]);
        }
        return injectionTargets;
    }

    @SuppressWarnings("unchecked")
    private BindingKey<T> createBindingKey(Type genericType, Annotation[] annotations) {
        Type rawType = genericType instanceof ParameterizedType ?
                ((ParameterizedType) genericType).getRawType() : genericType;
        this.optionalType = Optional.class.equals(rawType);
        if (this.optionalType) {
            genericType = getTypeArgument(genericType);
            rawType = genericType instanceof ParameterizedType ?
                    ((ParameterizedType) genericType).getRawType() : genericType;
        }
        boolean isMultiBinder = List.class.equals(rawType) || Collection.class.equals(rawType)
                || Iterable.class.equals(rawType);
        if (isMultiBinder) {
            genericType = getTypeArgument(genericType);
            rawType = genericType instanceof ParameterizedType ?
                    ((ParameterizedType) genericType).getRawType() : genericType;
        }
        boolean isProviderType = Provider.class.equals(rawType);
        Class<T> bindingType = (Class<T>) (isProviderType ? getTypeArgument(genericType) : rawType);
        BindingKey<T> bindingKey = BindingKey.of(bindingType, getQualifierAnnotation(annotations));
        bindingKey = isMultiBinder ? (BindingKey<T>) bindingKey.toListType() : bindingKey;
        return isProviderType ? (BindingKey<T>) bindingKey.toProviderType() : bindingKey;
    }

    private static Type getTypeArgument(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs != null && typeArgs.length == 1) {
                return typeArgs[0];
            }
        }
        return genericType;
    }

    private static Annotation getQualifierAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                return annotation;
            }
        }
        return null;
    }

    private static boolean allowsNullValue(Annotation[] annotations) {
        for (Annotation a : annotations) {
            Class<? extends Annotation> type = a.annotationType();
            if ("Nullable".equals(type.getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}
