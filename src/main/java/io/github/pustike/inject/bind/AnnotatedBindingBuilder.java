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
package io.github.pustike.inject.bind;

import java.lang.annotation.Annotation;

/**
 * A binding builder, which allows to specify annotations, or annotation types as constraints, depending on which
 * binding targets may, or may not be injected.
 * @param <T> the type of the class specified in this binding
 */
public interface AnnotatedBindingBuilder<T> extends LinkedBindingBuilder<T> {
    /**
     * Specifies that the binding can only be used for injection, if a field is annotated with
     * {@link javax.inject.Named} and has the given name.
     *
     * For ex: with the following binding specification:
     * <pre>{@code binder.bind(Tire.class).named("spare").to(SpareTire.class); }</pre>
     *
     * And this the named instance can be injected using the following annotation:
     * <pre>{@code @Inject @Named("spare") Tire spareTire; }</pre>
     * @param name the named value used to bind
     * @return the linked binding builder
     */
    LinkedBindingBuilder<T> named(String name);

    /**
     * Specifies, that the binding can only be used for injection, if a field is annotated with a qualifier
     * {@link Annotation} of the given type.
     *
     * For ex: with the following binding specification:
     * <pre>{@code binder.bind(Seat.class).annotatedWith(Drivers.class).to(DriversSeat.class); }</pre>
     *
     * And this the qualified instance can be injected using the following annotation:
     * <pre>{@code @Inject @Drivers Seat driversSeatA; }</pre>
     * @param annotationType the type of annotation to bind
     * @return the linked binding builder
     */
    LinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Specifies, that the binding can only be used for injection, if a field is annotated with an
     * {@link Annotation}, that equals the given. In general, this means that the annotation type, and all attributes
     * are equal.
     * @param annotation the annotation used to bind
     * @return the linked binding builder
     * @see Annotation#equals(Object)
     */
    LinkedBindingBuilder<T> annotatedWith(Annotation annotation);
}
