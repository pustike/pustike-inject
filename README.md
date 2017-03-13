Pustike Inject
==============
Pustike Inject is a simple dependency injection framework that implements the [JSR-330](http://javax-inject.github.io/javax-inject) specification.

Following are some of its key features:
* Programmatic configuration in plain Java using EDSL similar to that of [Guice Binder](http://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/Binder.html).
* Field, Method and Constructor injections that can be Named or Annotated specifically
* Default Scopes: Prototype, Lazy Singleton and Eager Singleton
* Support for custom scopes: Thread Local Scope and HTTP Session Scope
* BindingListener: useful for performing further configurations
* Only ~40kB in size and no external dependencies
* It requires Java 8 or higher.

**Documentation:** [Latest javadocs](http://pustike.github.io/pustike-inject/docs/latest/api/)

**Latest Release:** The most recent release is v1.0.0, released on March 07, 2017.

To add a dependency using Maven, use the following:
```xml
<dependency>
    <groupId>io.github.pustike</groupId>
    <artifactId>pustike-inject</artifactId>
    <version>1.0.0</version>
</dependency>
```
To add a dependency using Gradle:
```
dependencies {
    compile 'io.github.pustike:pustike-inject:1.0.0'
}
```

Injector
-------
Injector is a core part of the library and tracks all dependencies for all types configured by module binders. When an instance of a type or of a binding key is requested, the injector returns an instance by creating it and injecting all its declared dependencies (fields and constructor/methods). 

To create an injector, factory methods of Injectors class should be used, with bindings specified by modules and optionally with an injection point loader. And these bindings, specified by modules, are scanned to identify injection points and are registered as bindings linked to the binding key.

Following sample code shows, how an injector can be created with bindings defined in the module and how an instance of a given type can be obtained. 
```java
Module module = binder -> {
     binder.bind(Service.class).to(ServiceImpl.class).in(Singleton.class);
     ...
};
Injector injector = Injectors.create(module);
Service service = injector.getInstance(Service.class);
```
#### Module 
A module contributes configuration information, i.e interface bindings, which will be used to create an Injector. Its configure method is called to create bindings, during injector creation.

#### Binder
Binder collects configuration information (primarily bindings) which will be used to create an Injector. The Binder is passed as an argument to modules and each of them contribute their own bindings to injector using the binder.

Bindings are defined using EDSL(embedded domain-specific language) in plain Java with the help of following builders:  
* **Annotated Binding Builder**: It allows to specify annotations, or annotation types as constraints, depending on which binding targets may or may not be injected.
  1. *Named Bindings*: Specifies that the binding can only be used for injection, if a field is annotated with @Named and has the given name. For ex:
    ```java
    // with the following binding specification
    binder.bind(Tire.class).named("spare").to(SpareTire.class);
    ...
    // This the named instance can be injected using the following annotation:
    @Inject @Named("spare") Tire spareTire; 
    ```

  2. *Annotated Bindings*: Specifies that the binding can only be used for injection, if a field is annotated with a qualifier Annotation of the given type. For ex: 
    ```java
    // with the following binding specification
    binder.bind(Seat.class).annotatedWith(Drivers.class).to(DriversSeat.class);
    ...
    // It can be injected using the following annotation:
    @Inject @Drivers Seat driversSeatA;
    ```
* **Linked Binding Builder**: It allows to specify a bindings target which is the value, that gets injected, if the binding is applied.
  1. *To Implementation*: Binds the interface to the implementation as the target which is provisioned by the injector. For ex:
    ```java
    binder.bind(Service.class).to(ServiceImpl.class);
    ```

  2. *To Provider*: Binds the interface to a provider instance which provides instances of the target. For ex:
    ```java
    binder.bind(Service.class).toProvider(new ServiceProvider());

    // Or to a provider class which will created during injection
    binder.bind(Service.class).toProvider(ServiceProvider.class);
    ```

  3. *To Instance*: Specifies the binding target to be the specified instance. For ex:
    ```java
    ServiceImpl serviceImpl = new ServiceImpl();
    binder.bind(Service.class).to(serviceImpl); 
    ```

  4. *To Constructor*: Binds the interface to constructor of the implementation which is used create new instances by the injector. It is useful for cases where existing classes cannot be modified and it is a bit simpler than using a Provider. For ex:
    ```java
    Constructor<?> loneConstructor = ServiceImpl.class.getDeclaredConstructors()[0];
    binder.bind(Service.class).toConstructor(loneConstructor);
    ```

  5. *To Factory Method*: Binds the interface to the factory method which is used create new instances by the injector. It is useful for cases where existing classes cannot be modified and it is a bit simpler than using a Provider. For ex:
    ```java
    Method factoryMethod = ServiceImpl.class.getMethod("create");
    binder.bind(Service.class).to(factoryMethod);
    ```

* **Scoped Binding Builder**: By default, if no scope annotation is present, the injector creates an instance (by injecting the type's constructor), uses the instance for one injection, and then forgets it. If a scope annotation is present, the injector may retain the instance for possible reuse in a later injection. 
    In addition to Prototype scope, following additional scopes are supported.
  1. *Prototype scope*: Prototype or per-call scope is the default scope, in every module, which means a new object is created every time it will be injected somewhere. For ex: 
    ```java
    binder.bind(Service.class).to(ServiceImpl.class);
    ```
    The default scope is applied to bindings which do not have any specific scope defined. This can be changed using the following api:
    ```java
    binder.setDefaultScope(Singleton.class);// to change the default scope to Singleton 
    ```
  
  2. *Lazy Singleton Scope*: Instructs the Injector to lazily initialize this singleton-scoped binding, i.e. only when it is requested or being injected.
    ```java
    binder.bind(Service.class).to(ServiceImpl.class).asLazySingleton();
    // or it can be configured using the annotation
    binder.bind(Service.class).to(ServiceImpl.class).in(Singleton.class);
    ```

  3. *Eager Singleton Scope*: A binding with this scope will create a single instance, immediately after the injector is configured.
    ```java
    binder.bind(Service.class).to(ServiceImpl.class).asEagerSingleton();
    ```

##### Creating Custom Scope
Custom Scopes can be created to retain the instance only in a certain context, like ```@SessionScoped, @ThreadScoped.``` The process of creating and using such a scope involves many steps. The following example shows how Session Scope can be defined which stores created instances as attributes in the session.
* Defining a scoping annotation
  ```java
  @Scope @Documented @Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
  public @interface SessionScoped { }
  ```

* Implementing the Scope interface
  ```java
  public final class SessionScope implements Scope {
      private static final ThreadLocal<HttpSession> threadLocal = new ThreadLocal<>();
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

      public static SessionScopeContext open(HttpSession session) {
          threadLocal.set(session);
          return threadLocal::remove;
      }

      /** Closeable subclass that does not throw any exceptions from close. */
      public interface SessionScopeContext extends Closeable {
          @Override void close();
      }
  }
  ```
    
* Binding the scope annotation to its implementation
  ```java
  Injector injector = Injectors.create((Module) binder -> {
     SessionScope sessionScope = new SessionScope();
     binder.bindScope(SessionScoped.class, sessionScope);
     binder.bind(Service.class).to(ServiceImpl.class).in(sessionScope);
  });
  ```

* Opening and closing the scope context
  ```java
  // open the session scope context on receiving the request with a http session
  SessionScopeContext scopeContext = SessionScope.open(httpSession);
  try {
      // process the request in this context
  } finally {
      scopeContext.close();
  }
  ```

##### Binding Listener
Binding listener is invoked after binding of the type is registered into injector. Useful for performing further configurations, such as, a MVC framework can register it as a controller if @Controller annotation is present. For ex:
```java
Predicate<Class<?>> predicate = targetType -> targetType.getDeclaredAnnotation(Controller.class) != null;
binder.addBindingListener(predicate, registerController());
binder.bind(HomeController.class);

// the above listener will match the following controller class
@Controller
public class HomeController {
    @RequestMapping("/")
    public String redirectToHome() {
        return "redirect:/home";
    }
}
```
##### Injection Listener
Injection Listener listens for new instances created by injector, it is invoked after the instance's fields and methods are injected. It is useful for performing post-injection initialization.

##### Injection Point Loader
This is an interface for loading injection points (fields and methods/constructor) created by scanning through target types specified in bindings. It also provides an utility method to create injection points by reflectively scanning through target types. The default internal implementation stores these injection points in a ConcurrentHashMap.

* Custom Injection Point Loader:
 Custom implementations can be created to use an advanced backing cache to store these injection points. Following is a sample custom injection point loader that uses [Caffeine](https://github.com/ben-manes/caffeine) for caching injection points.
  ```java
  public class CaffeineInjectionPointLoader implements InjectionPointLoader {
        private final LoadingCache<Class<?>, List<InjectionPoint<Object>>> injectionPointCache;

        public CaffeineInjectionPointLoader() {
            this.injectionPointCache = Caffeine.newBuilder().weakValues().build(this::createInjectionPoints);
        }

        @Override
        public List<InjectionPoint<Object>> getInjectionPoints(Class<?> clazz) {
            return injectionPointCache.get(clazz);
        }

        @Override
        public void invalidateAll() {
            injectionPointCache.invalidateAll();
        }
  }
  ```
  And when creating the injector, this custom loader should be used as: 
  ```java
     CaffeineInjectionPointLoader injectionPointLoader = new CaffeineInjectionPointLoader();
     Iterable<Module> modules = ...
     Injector injector = Injectors.create(injectionPointLoader, modules);
  ```

Roadmap
-------
* List Bindings to support multiple bindings for the same key
* Better Exception handling and error reporting
* Bug fixes and writing more test cases
* More detailed documentation and examples
* Optional bindings and Lazy Injections, as available in [Dagger](https://github.com/google/dagger)

Other JSR-330 spec Implementations
---------------------------------
The following projects are very widely used: 
* [Guice](https://github.com/google/guice) (pronounced 'juice') is a lightweight dependency injection framework. And this is also the reference implementation of JSR-330 spec.
* [Spring Framework](https://github.com/spring-projects/spring-framework) provides a comprehensive programming and configuration model for modern Java-based enterprise applications.

And there are also many other implementations available, like:
* [Dagger](https://github.com/google/dagger) is A fast dependency injector for Android and Java.
* [HK2](https://hk2.java.net) is a light-weight and dynamic dependency injection framework.
* [Commons Inject](https://commons.apache.org/sandbox/commons-inject): This project is not active anymore, but some of Pustike Inject's APIs were initially derived from here.

License
-------
This library is published under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
```
 Copyright (C) 2016-2017 the original author or authors.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
```
