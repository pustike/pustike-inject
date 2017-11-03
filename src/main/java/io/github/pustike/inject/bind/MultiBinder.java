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

/**
 * An API to bind multiple values separately, to later inject them as a complete collection.
 * For ex.
 * <pre>{@code
 * Module snacksModule = binder -> {
 *     MultiBinder<Snack> multiBinder = binder.multiBinder(Snack.class);
 *     multiBinder.addBinding().toInstance(new Twix());
 *     multiBinder.addBinding().toProvider(SnickersProvider.class);
 *     multiBinder.addBinding().to(Skittles.class);
 * };
 * }</pre>
 *
 * With this binding, a {@link java.util.List}{@code <Snack>} can now be injected, as:
 *
 * <pre><code>
 * class SnackMachine {
 *   {@literal @}Inject
 *   public SnackMachine(List&lt;Snack&gt; snacks) { ... }
 * }</code></pre>
 *
 * If desired, {@link java.util.Collection}{@code <Provider<Snack>>} can also be injected.
 *
 * <p>Contributing multiBindings from different modules is supported. For example, it is okay for both {@code
 * CandyModule} and {@code ChipsModule} to create their own {@code MultiBinder<Snack>}, and to each contribute
 * bindings to the list of snacks. When that list is injected, it will contain elements from both modules.
 *
 * <p>The injected list is unmodifiable and elements can only be added to the list by configuring the multiBinder.
 * Elements can not be removed from the list.
 *
 * <p>Annotations can be used to create different lists of the same element type. Each distinct annotation gets its
 * own independent collection of elements.
 * @param <T> the type of the class specified in this multiBinder
 */
public interface MultiBinder<T> {
    /**
     * Returns the {@link LinkedBindingBuilder} used to add a new element into the list.
     *
     * <p>It is an error to call this method without also calling one of the {@code to} methods on the
     * returned binding builder.
     *
     * <p>Scoping elements independently is supported. Use the {@code in} method to specify a binding
     * scope.
     * @return the linked binding builder
     */
    LinkedBindingBuilder<T> addBinding();
}
