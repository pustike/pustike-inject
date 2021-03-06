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
package io.github.pustike.inject.spi;

import io.github.pustike.inject.BindingKey;

/**
 * Binding listener is invoked after binding of the type is registered into injector. Useful for performing further
 * configurations. For ex: a MVC framework can register it as a controller if @Controller annotation is present.
 */
@FunctionalInterface
public interface BindingListener {
    /**
     * Called after the targetClass binding to the key as specified in the module is registered to the injector.
     * @param bindingKey the binding key, for which a binding has been registered.
     * @param targetClass the target class specified in the binding
     */
    void afterBinding(BindingKey<?> bindingKey, Class<?> targetClass);
}
