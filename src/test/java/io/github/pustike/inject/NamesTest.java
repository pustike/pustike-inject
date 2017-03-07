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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Named;

import io.github.pustike.inject.bind.Module;
import junit.framework.TestCase;

/**
 * This class is borrowed from Guice project's
 * <a href="https://github.com/google/guice/blob/master/core/test/com/google/inject/name/NamesTest.java" target="_blank">
 * NamesTest</a> class with few modifications.
 * </p>
 * @author jessewilson@google.com (Jesse Wilson)
 */
public class NamesTest extends TestCase {
    @Named("foo")
    private String foo;
    private Named namedFoo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        namedFoo = getClass().getDeclaredField("foo").getAnnotation(Named.class);
    }

    /**
     * Fails unless {@code expected.equals(actual)}, {@code
     * actual.equals(expected)} and their hash codes are equal. This is useful
     * for testing the equals method itself.
     */
    public static void assertEqualsBothWays(Object expected, Object actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals("expected.equals(actual)", actual, expected);
        assertEquals("actual.equals(expected)", expected, actual);
        assertEquals("hashCode", expected.hashCode(), actual.hashCode());
    }

    /**
     * Fails unless {@code object} doesn't equal itself when reserialized.
     */
    public static void assertEqualWhenReserialized(Object object)
            throws IOException {
        Object reserialized = reserialize(object);
        assertEquals(object, reserialized);
        assertEquals(object.hashCode(), reserialized.hashCode());
    }

    public static <E> E reserialize(E original) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(original);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            @SuppressWarnings("unchecked") // the reserialized type is assignable
                    E reserialized = (E) new ObjectInputStream(in).readObject();
            return reserialized;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void testConsistentEqualsAndHashcode() {
        Named actual = Names.named("foo");
        assertEqualsBothWays(namedFoo, actual);
        assertEquals(namedFoo.toString(), actual.toString());
    }

    public void testNamedIsSerializable() throws IOException {
        assertEqualWhenReserialized(Names.named("foo"));
    }

    public void testBindPropertiesUsingProperties() {
        final Properties teams = new Properties();
        teams.setProperty("SanJose", "Sharks");
        teams.setProperty("Edmonton", "Oilers");

        Module module = binder -> Names.bindProperties(binder, teams);
        Injector injector = Injectors.create(module);

        assertEquals("Sharks", injector.getInstance(BindingKey.of(String.class, "SanJose")));
        assertEquals("Oilers", injector.getInstance(BindingKey.of(String.class, "Edmonton")));
    }

    public void testBindPropertiesUsingMap() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("SanJose", "Sharks");
        properties.put("Edmonton", "Oilers");

        Module module = binder -> Names.bindProperties(binder, properties);
        Injector injector = Injectors.create(module);

        assertEquals("Sharks", injector.getInstance(BindingKey.of(String.class, "SanJose")));
        assertEquals("Oilers", injector.getInstance(BindingKey.of(String.class, "Edmonton")));
    }

    public void testBindPropertiesIncludesInheritedProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("Edmonton", "Eskimos");
        defaults.setProperty("Regina", "Pats");

        final Properties teams = new Properties(defaults);
        teams.setProperty("SanJose", "Sharks");
        teams.setProperty("Edmonton", "Oilers");

        Module module = binder -> Names.bindProperties(binder, teams);
        Injector injector = Injectors.create(module);

        assertEquals("Pats", injector.getInstance(BindingKey.of(String.class, "Regina")));
        assertEquals("Oilers", injector.getInstance(BindingKey.of(String.class, "Edmonton")));
        assertEquals("Sharks", injector.getInstance(BindingKey.of(String.class, "SanJose")));

        try {
            injector.getInstance(BindingKey.of(String.class, "Calgary"));
            fail();
        } catch (RuntimeException expected) {
        }
    }
}
