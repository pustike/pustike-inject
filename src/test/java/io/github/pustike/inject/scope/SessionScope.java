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
package io.github.pustike.inject.scope;

import java.io.Closeable;
import javax.inject.Provider;
import javax.servlet.http.HttpSession;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;

/**
 * Session Scope that stores created instances as attributes in the session.
 * Example usage:
 * <pre><code>
 * Injector injector = Injectors.create((Module) binder -&gt; {
 *     SessionScope sessionScope = new SessionScope();
 *     binder.bindScope(SessionScoped.class, sessionScope);
 *     binder.bind(SomeClass.class).in(sessionScope);
 * });
 * ...
 * SessionScopeContext scopeContext = SessionScope.open(httpSession);
 * ...
 * scopeContext.close();
 * </code></pre>
 */
public final class SessionScope implements Scope {
    private static final ThreadLocal<HttpSession> threadLocal = new ThreadLocal<>();

    /** A sentinel attribute value representing null. */
    private enum NullObject { INSTANCE }

    @Override
    public <T> Provider<T> scope(BindingKey<T> bindingKey, Provider<T> creator) {
        final String name = bindingKey.toString();
        return () -> {
            HttpSession session = threadLocal.get();
            if (session == null) {
                throw new IllegalStateException("Session is not open in this scope, for the key:" + name);
            }
            synchronized (session) {
                Object obj = session.getAttribute(name);
                if (NullObject.INSTANCE == obj) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                T t = (T) obj;
                if (t == null) {
                    t = creator.get();
                    session.setAttribute(name, (t != null) ? t : NullObject.INSTANCE);
                }
                return t;
            }
        };
    }

    @Override
    public String toString() {
        return SessionScoped.class.getName();
    }

    /**
     * Sets the session object to the local thread and returns a closeable handle that should be closed
     * to clear the context.
     * <p/>
     * Preferably, call the close method in a finally block to make sure that it executes,
     * to avoid possible memory leaks.
     */
    public static SessionScopeContext open(HttpSession session) {
        threadLocal.set(session);
        return threadLocal::remove;
    }

    /** Closeable subclass that does not throw any exceptions from close. */
    public interface SessionScopeContext extends Closeable {
        @Override void close();
    }
}
