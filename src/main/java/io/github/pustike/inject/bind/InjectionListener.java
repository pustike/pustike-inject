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

import io.github.pustike.inject.BindingKey;

/**
 * Listens for new instances created by injector, invoked after it's fields and methods are injected. Useful for
 * performing post-injection initialization.
 */
public interface InjectionListener {
    /**
     * Invoked after fields and methods of the instance ar injected by the injector.
     * @param bindingKey the binding key configured for this instance
     * @param instance   the newly created instance after dependency injection
     */
    void afterInjection(BindingKey<?> bindingKey, Object instance);
}
