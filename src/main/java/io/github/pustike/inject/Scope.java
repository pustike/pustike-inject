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

import javax.inject.Provider;

/**
 * Interface to implement custom scopes.
 */
public interface Scope {
    /**
     * Scopes a provider. The returned provider returns objects from this scope. If an object does not exist in this
     * scope, the provider can use the given unscoped provider to retrieve one.
     *
     * <p>Scope implementations are strongly encouraged to override {@link Object#toString} in the returned provider
     * and include the backing provider's {@code toString()} output.
     * @param bindingKey binding key
     * @param creator    locates an instance when one doesn't already exist in this scope
     * @return a new provider which only delegates to the given unscoped provider when an instance of the requested
     * object doesn't already exist in this scope
     * @param <T> the type of the instance being scoped
     */
    <T> Provider<T> scope(BindingKey<T> bindingKey, Provider<T> creator);

    /**
     * A short but useful description of this scope.
     */
    String toString();
}
