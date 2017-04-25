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
package io.github.pustike.inject;

import java.lang.annotation.Annotation;
import java.util.Objects;
import javax.inject.Named;

/**
 * Binding key that contains an injection type and an optional qualifier annotation. It is also used to match the type
 * and qualifier annotation at the point of injection.
 * @param <T> the type of the class specified in this key
 */
public final class BindingKey<T> {
    // the injection type
    private final Class<T> type;
    // qualifier annotation
    private final Annotation annotation;
    // type of qualifier annotation
    private final Class<? extends Annotation> annotationType;
    // indicates that this key matches to a provider of the type
    private boolean providerKey;
    // hash code of this binding key, computed lazily using all specified parameters
    private int hashCode;

    /**
     * Constructs a new Binding Key for the specified type, the qualifier annotation and annotation type.
     * @param type           the injection class
     * @param annotation     the qualifying annotation defined for the type
     * @param annotationType the type of qualifier annotation defined for the type
     */
    private BindingKey(Class<T> type, Annotation annotation, Class<? extends Annotation> annotationType) {
        this.type = Objects.requireNonNull(type);
        this.annotation = annotation;
        this.annotationType = annotationType;
    }

    /**
     * Constructs a new Binding Key for the specified type.
     * @param type the injection class
     * @param <T>  the type of of the class modeled by this key
     * @return a new key with the specified type and no qualifying annotation
     */
    public static <T> BindingKey<T> of(Class<T> type) {
        return new BindingKey<>(type, null, null);
    }

    /**
     * Constructs a new Binding Key for the specified type and name.
     * @param type  the injection class
     * @param named the qualifying name defined for the type
     * @param <T>   the type of of the class modeled by this key
     * @return a new key with the specified type and the named qualifier
     */
    public static <T> BindingKey<T> of(Class<T> type, String named) {
        return new BindingKey<>(type, Names.named(named), null);
    }

    /**
     * Constructs a new Binding Key for the specified type and the qualifier annotation.
     * @param type       the injection class
     * @param annotation the qualifying annotation defined for the type
     * @param <T>        the type of of the class modeled by this key
     * @return a new key with the specified type and the qualifier annotation
     */
    public static <T> BindingKey<T> of(Class<T> type, Annotation annotation) {
        return new BindingKey<>(type, annotation, annotation == null ? null : annotation.annotationType());
    }

    /**
     * Constructs a new Binding Key for the specified type and the qualifier annotation.
     * @param type           the injection class
     * @param annotationType the type of qualifier annotation defined for the type
     * @param <T>            the type of of the class modeled by this key
     * @return a new key with the specified type and the type of qualifier annotation
     */
    public static <T> BindingKey<T> of(Class<T> type, Class<? extends Annotation> annotationType) {
        return new BindingKey<>(type, null, annotationType);
    }

    /**
     * Get the injection type of this key.
     * @return the injection type of this key
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * Get the the qualifier annotation specified in this key.
     * @return the qualifier annotation
     */
    private Annotation getAnnotation() {
        return annotation;
    }

    /**
     * Get the the type of qualifier annotation specified in this key.
     * @return the type of qualifier annotation
     */
    private Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }

    /**
     * Create a new key to indicate that this key matches to a {@link javax.inject.Provider} of the type.
     * @return a new key that matches to the provider of this binding type and qualifier
     */
    public BindingKey<T> createProviderKey() {
        BindingKey<T> providerKey = new BindingKey<>(type, annotation, annotationType);
        providerKey.hashCode = hashCode;
        providerKey.providerKey = true;
        return providerKey;
    }

    /**
     * Returns true if this key is of {@link javax.inject.Provider} type.
     * @return true if this key is of provider type.
     */
    public boolean isProviderKey() {
        return providerKey;
    }

    /**
     * Uses the computed hash code of this key to check if the given object equals.
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise
     * @see #hashCode()
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BindingKey<?>)) {
            return false;
        }
        BindingKey<?> other = (BindingKey<?>) obj;
        return hashCode() == other.hashCode();
    }

    /**
     * Computes the hash code using hash codes of the binding type and qualifier annotation or annotation type.
     * @return the hash code value for this key
     */
    @Override
    public int hashCode() {
        return hashCode == 0 ? hashCode = computeHashCode() : hashCode;
    }

    /**
     * Computes the hash code for this key.
     */
    private int computeHashCode() {
        int result = type.hashCode();
        if (annotation != null) {
            result = 31 * result + annotation.hashCode();
        } else if (annotationType != null) {
            result = 31 * result + AnnotationUtils.hashCode(annotationType);
        }
        return result;
    }

    /**
     * Generates a string representation of the key using the binding type and qualifier annotation.
     * @return a string representation of the binding key
     */
    @Override
    public String toString() {
        return toString(this);
    }

    private static String toString(BindingKey<?> key) {
        if (key == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder().append(key.getType().getName());
        Annotation annotation = key.getAnnotation();
        if (annotation != null) {
            if (annotation instanceof Named) {
                sb.append("@javax.inject.Named(value=").append(((Named) annotation).value()).append(')');
            } else {
                sb.append('@').append(annotation);
            }
        } else if (key.getAnnotationType() != null) {
            sb.append('@').append(key.getAnnotationType());
        }
        return sb.toString();
    }
}
