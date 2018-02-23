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
package io.github.pustike.inject.scope;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Module;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Thread Scope Tests.
 * Source: https://github.com/google/guice/issues/114
 */
public class ThreadScopeTest {
    public static class SomeClass {
        @Inject
        public SomeClass() {
        }
    }

    private static final Injector injector = Injectors.create((Module) binder -> {
        ThreadScope threadScope = new ThreadScope();
        binder.bindScope(ThreadScoped.class, threadScope);
        binder.bind(ThreadScope.class).toInstance(threadScope);
        binder.bind(SomeClass.class).in(threadScope);
    });

    @Test
    public void testReset() {
        SomeClass someClass = injector.getInstance(SomeClass.class);
        assertSame(someClass, injector.getInstance(SomeClass.class));
        injector.getInstance(ThreadScope.class).clearContext();
        assertNotSame(someClass, injector.getInstance(SomeClass.class));
    }

    @Test
    public void testLocality() {
        SomeClass someClass = injector.getInstance(SomeClass.class);
        final SomeClass[] innerSomeClass = new SomeClass[1];
        final CountDownLatch done = new CountDownLatch(1);

        new Thread(() -> {
            innerSomeClass[0] = injector.getInstance(SomeClass.class);
            done.countDown();
        }).start();

        try {
            done.await();
        } catch (InterruptedException e) {
            Assert.fail("unexpected thread interruption");
        }

        assertNotSame(someClass, innerSomeClass[0]);
    }

    @Test
    public void testConcurrency() {
        final CountDownLatch done = new CountDownLatch(1);
        Executor executor = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 200; i++) {
            final int index = i;
            executor.execute(() -> {
                assertSame(injector.getInstance(SomeClass.class), injector.getInstance(SomeClass.class));
                injector.getInstance(ThreadScope.class).clearContext();
                if (index == 199)
                    done.countDown();
            });
        }
        try {
            done.await();
        } catch (InterruptedException e) {
            Assert.fail("unexpected thread interruption");
        }
    }
}
