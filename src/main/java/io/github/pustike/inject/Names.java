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
package io.github.pustike.inject;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import jakarta.inject.Named;

import io.github.pustike.inject.bind.Binder;

/**
 * Utility methods for use with {@code @}{@link Named}.
 *
 * <p>This class is borrowed from Guice project's
 * <a href="https://github.com/google/guice/blob/master/core/src/com/google/inject/name/Names.java" target="_blank">
 * Names</a> class with few modifications.
 * </p>
 * @author crazybob@google.com (Bob Lee)
 */
public final class Names {

    private Names() {
    }

    /**
     * Creates a {@link Named} annotation with {@code name} as the value.
     * @param name the named value
     * @return the named instance
     */
    public static Named named(String name) {
        return new NamedImpl(name);
    }

    /**
     * Creates a constant binding to {@code @Named(key)} for each entry in {@code keyValueMap}.
     * @param binder      the binder used to create bindings
     * @param keyValueMap the map with [key, value] entries to bind
     */
    public static void bindProperties(Binder binder, Map<String, String> keyValueMap) {
        for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
            binder.bind(String.class).named(entry.getKey()).toInstance(entry.getValue());
        }
    }

    /**
     * Creates a constant binding to {@code @Named(key)} for each property.
     * This method binds all properties including those inherited from {@link Properties#defaults defaults}.
     * @param binder     the binder used to create bindings
     * @param properties the properties object with [key, value] entries to bind
     */
    public static void bindProperties(Binder binder, Properties properties) {
        // use enumeration to include the default properties
        for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
            String propertyName = (String) e.nextElement();
            String value = properties.getProperty(propertyName);
            binder.bind(String.class).named(propertyName).toInstance(value);
        }
    }

    /**
     * Implementation of {@link jakarta.inject.Named} to create instances of this annotation.
     */
    private static final class NamedImpl implements Named, Serializable {
        private static final long serialVersionUID = 0L;
        private static final boolean isJava11 = System.getProperty("java.version").startsWith("11");
        private final String value;

        NamedImpl(String value) {
            this.value = Objects.requireNonNull(value, "name");
        }

        @Override
        public String value() {
            return this.value;
        }

        @Override
        public int hashCode() {
            // This is specified in java.lang.Annotation.
            return (127 * "value".hashCode()) ^ value.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Named && value.equals(((Named) o).value());
        }

        @Override
        public String toString() {
            // annotation's toString has "value=" in Java 11 but not in Java 14!
            return '@' + Named.class.getName() + '(' + (isJava11 ? "value=" : "") + "\"" + value + "\")";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Named.class;
        }
    }
}
