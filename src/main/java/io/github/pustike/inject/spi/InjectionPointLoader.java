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

import java.util.List;
import java.util.function.Function;

/**
 * Strategy interface for loading injection points (to fields and methods/constructor) created by reflectively scanning
 * through target types.
 * <p>
 * It also provides an utility method to create injection points by reflectively scanning through target types.
 * The default internal implementation stores these injection points in a ConcurrentHashMap, but custom
 * implementations can use an advanced backing cache to store them. Following is a sample custom injection point
 * loader used when creating the injector:
 * <pre><code>
 * CaffeineInjectionPointLoader injectionPointLoader = new CaffeineInjectionPointLoader();
 * Iterable&lt;Module&gt; modules = ...
 * Injector injector = Injectors.create(injectionPointLoader, modules);
 *
 * // A custom injection point loader that uses Caffeine as backing cache to store injection points.
 * public class CaffeineInjectionPointLoader implements InjectionPointLoader {
 *      private final Cache&lt;Class&lt;?&gt;, List&lt;InjectionPoint&lt;Object&gt;&gt;&gt; cache;
 *
 *      public CaffeineInjectionPointLoader() {
 *          this.cache = Caffeine.newBuilder().weakValues().build();
 *      }
 *
 *      &#064;Override
 *      public List&lt;InjectionPoint&lt;Object&gt;&gt; getInjectionPoints(Class&lt;?&gt; clazz,
 *              Function&lt;Class&lt;?&gt;, List&lt;InjectionPoint&lt;Object&gt;&gt;&gt; creator) {
 *          return cache.get(clazz, creator);
 *      }
 *
 *      &#064;Override
 *      public void invalidateAll() {
 *          cache.invalidateAll();
 *      }
 * }
 * </code></pre>
 */
public interface InjectionPointLoader {
    /**
     * Get injection points in the given clazz. If the given class is already scanned for injection points, return
     * the resulting list from cache, else load and ret the data.
     * @param clazz the class to be scanned for injection points
     * @param creator the function to create list of Injection Points
     * @return a list of injection points for given class
     */
    List<InjectionPoint<Object>> getInjectionPoints(Class<?> clazz,
            Function<Class<?>, List<InjectionPoint<Object>>> creator);

    /**
     * Clears all cached injection points data, invoked when injector is disposed.
     */
    void invalidateAll();
}
