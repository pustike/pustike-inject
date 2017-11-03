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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.bind.InjectionPoint;
import io.github.pustike.inject.bind.InjectionPointLoader;

/**
 * Default Injection Point Loader.
 */
public final class DefaultInjectionPointLoader implements InjectionPointLoader {
    private final Map<Class<?>, List<InjectionPoint<Object>>> injectionPointCache;

    DefaultInjectionPointLoader() {
        this.injectionPointCache = new ConcurrentHashMap<>();
    }

    @Override
    public List<InjectionPoint<Object>> getInjectionPoints(Class<?> clazz) {
        return injectionPointCache.computeIfAbsent(clazz, this::createInjectionPoints);
    }

    @Override
    public void invalidateAll() {
        injectionPointCache.clear();
    }

    public static List<InjectionPoint<Object>> doCreateInjectionPoints(final Class<?> targetClass) {
        List<InjectionPoint<Object>> injectionPointList = new LinkedList<>();
        Set<Integer> visitedMethodHashCodeSet = new HashSet<>();
        for (Class clazz = targetClass; clazz != null; clazz = clazz.getSuperclass()) {
            if (clazz == Object.class) {
                continue;
            }
            int index = 0, staticIndex = 0;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getDeclaredAnnotation(Inject.class) != null) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        injectionPointList.add(staticIndex++, createInjectionPoint(field));
                    } else {
                        injectionPointList.add(index, createInjectionPoint(field));
                    }
                    index++;
                }
            }
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                int hashCode = computeHashCode(clazz, method);
                if (visitedMethodHashCodeSet.contains(hashCode)) {
                    continue;
                }
                visitedMethodHashCodeSet.add(hashCode);
                if (method.getDeclaredAnnotation(Inject.class) != null) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        injectionPointList.add(staticIndex++, createInjectionPoint(method));
                    } else {
                        injectionPointList.add(index, createInjectionPoint(method));
                    }
                    index++;
                }
            }
        }
        return injectionPointList;
    }

    static <T> InjectionPoint<T> createInjectionPoint(Class<? extends T> targetType) {
        @SuppressWarnings("unchecked")
        Constructor<T>[] constructors = (Constructor<T>[]) targetType.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new RuntimeException("No constructors available for type: " + targetType);
        }
        Constructor<T> defaultConstructor = null;
        for (Constructor<T> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                return createInjectionPoint(constructor);
            }
            if (constructor.getParameterCount() == 0) {
                defaultConstructor = constructor;
            }
        }
        if (defaultConstructor == null) {
            throw new RuntimeException("No constructors available for type: " + targetType);
        }
        return createInjectionPoint(defaultConstructor);
    }

    static <T> InjectionPoint<T> createInjectionPoint(Executable executable) {
        Type[] parameterTypes = executable.getGenericParameterTypes();
        Annotation[][] annotations = executable.getParameterAnnotations();
        BindingKey<T>[] parameterBindingKeys = createParameterBindingKeys(parameterTypes, annotations);
        Boolean[] nullableParams = new Boolean[parameterTypes.length];
        Arrays.setAll(nullableParams, i -> allowsNullValue(annotations[i]));
        return new ExecutableInjectionPoint<>(executable, parameterBindingKeys, nullableParams);
    }

    @SuppressWarnings("unchecked")
    private static <T> InjectionPoint<T> createInjectionPoint(Field field) {
        Annotation[] annotations = field.getAnnotations();
        BindingKey<T> targetKey = createBindingKey(field.getGenericType(), annotations);
        return new FieldInjectionPoint<>(field, targetKey, allowsNullValue(annotations));
    }

    @SuppressWarnings("unchecked")
    private static <T> BindingKey<T>[] createParameterBindingKeys(Type[] parameterTypes, Annotation[][] annotations) {
        BindingKey<T>[] bindingKeys = (BindingKey<T>[]) Array.newInstance(BindingKey.class, parameterTypes.length);
        Arrays.setAll(bindingKeys, i -> createBindingKey(parameterTypes[i], annotations[i]));
        return bindingKeys;
    }

    @SuppressWarnings("unchecked")
    static <T> BindingKey<T> createBindingKey(Type genericType, Annotation[] annotations) {
        Type rawType = genericType instanceof ParameterizedType ?
                ((ParameterizedType) genericType).getRawType() : genericType;
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

    private static int computeHashCode(Class clazz, Method method) {
        int hashCode = 31 + method.getName().hashCode();
        for (Class<?> parameterType : method.getParameterTypes()) {
            hashCode = 31 * hashCode + parameterType.hashCode();
        }
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)
                && !Modifier.isPrivate(modifiers)) { // package-private
            hashCode = 31 * hashCode + clazz.getPackage().hashCode();
        } else if (Modifier.isPrivate(modifiers)) {
            hashCode = 31 * hashCode + clazz.hashCode(); // private method
        }
        return hashCode;
    }

    private static Annotation getQualifierAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                return annotation;
            }
        }
        return null;
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
