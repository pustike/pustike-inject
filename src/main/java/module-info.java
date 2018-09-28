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
/**
 * Implements <a href="https://javax-inject.github.io/javax-inject" target="_blank"> JSR-330</a> :
 * <i>Dependency Injection for Java</i> specification.
 */
module io.github.pustike.inject {
    requires java.inject;

    exports io.github.pustike.inject;
    exports io.github.pustike.inject.bind;
    exports io.github.pustike.inject.events;
    exports io.github.pustike.inject.spi;
}
