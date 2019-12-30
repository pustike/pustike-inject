Pustike Inject    [![][Maven Central img]][Maven Central] [![][Javadocs img]][Javadocs] [![][license img]][license] [![][GitHub Actions Status]]
==============
Pustike Inject is a simple dependency injection framework that implements the [JSR-330](https://javax-inject.github.io/javax-inject) specification.

Following are some of its key features:
* Programmatic configuration in plain Java using EDSL similar to that of [Guice Binder](https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/Binder.html).
* Field, Method and Constructor injections that can be Named or Annotated specifically
* Default Scopes: Prototype, Lazy Singleton and Eager Singleton
* Support for custom scopes: Thread Local Scope and HTTP Session Scope
* MultiBinder support to bind multiple values as List/Collection
* Hierarchical Injector support
* Optional dependencies using ```@Nullable``` or ```Optional<T>```
* BindingListener: useful for performing further configurations
* Events to allow publish-subscribe style communication between components
* Only ~60kB in size and no external dependencies
* It requires Java 11 or higher.

**Documentation:** Latest javadocs is available [here][Javadocs].

**Release:** The most recent release is v1.5.0 (2018-09-28).

To add a dependency using Maven, use the following:
```xml
<dependency>
    <groupId>io.github.pustike</groupId>
    <artifactId>pustike-inject</artifactId>
    <version>1.5.0</version>
</dependency>
```
To add a dependency using Gradle:
```
dependencies {
    compile 'io.github.pustike:pustike-inject:1.5.0'
}
```
Or, download the [latest JAR](https://search.maven.org/remote_content?g=io.github.pustike&a=pustike-inject&v=LATEST)

Injector
-------
Injector is the core part of this library and tracks all dependencies for all types configured by module binders. When an instance of a type or of a binding key is requested, the injector returns an instance by creating it and injecting all its declared dependencies (fields and constructor/methods). 

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
* **Hierarchical Injector**
Injector can create a child injector that delegates all requests for bindings, that are not found, to it's parent injector. All bindings in the parent injector are visible to the child, but elements of the child injector are not visible to its parent.
```java
    Injector parentInjector = Injectors.create(new ParentModule1(), new ParentModule2());
    Injector childInjector = parentInjector.createChildInjector(new ChildModule1(), new ChildModule2());
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
    // This named instance can be injected using the following annotation:
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

    // Or to a provider class which will be created during injection
    binder.bind(Service.class).toProvider(ServiceProvider.class);
    ```

  3. *To Instance*: Specifies the binding target to be the specified instance. For ex:
    ```java
    ServiceImpl serviceImpl = new ServiceImpl();
    binder.bind(Service.class).to(serviceImpl); 
    ```

  4. *To Constructor*: Binds the interface to constructor of the implementation which is used to create new instances by the injector. It is useful for cases where existing classes cannot be modified and it is a bit simpler than using a Provider. For ex:
    ```java
    Constructor<?> loneConstructor = ServiceImpl.class.getDeclaredConstructors()[0];
    binder.bind(Service.class).toConstructor(loneConstructor);
    ```

  5. *To Factory Method*: Binds the interface to the factory method which is used to create new instances by the injector. It is useful for cases where existing classes cannot be modified and it is a bit simpler than using a Provider. For ex:
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

* **Install Module**: Bindings can be installed from another module, which allows for composition. For instance, a FooModule may install FooServiceModule. This would mean that an Injector created based only on FooModule will include bindings and providers defined in both FooModule and FooServiceModule. But same module can not be installed more than once, as duplicate bindings are not allowed.
    ```java
    binder.install(new FooModule());
    ```

* **@Provides Methods**: Methods annotated with ```@Provides``` can be used when objects need to be created before binding them. This method must be defined within a module and it must have an ```@Provides``` annotation. This method's return type is the bound type and whenever the injector needs an instance of this type, it will invoke the method. It is similar to *ToInstance* bindings, but also supports injecting parameters to this method.
    ```java
    Module billingModule  = new Module() {
        @Override
        public void configure(Binder binder) {
            // ...
        }
    
        @Provides
        TransactionLog provideTransactionLog() {
            DatabaseTransactionLog transactionLog = new DatabaseTransactionLog();
            transactionLog.setJdbcUrl("jdbc:mysql://localhost/pizza");
            transactionLog.setThreadPoolSize(30);
            return transactionLog;
        }
    };
    ```

##### Using MultiBinder API
With MultiBinder API multiple values of a type can be bound separately, to later inject them as a complete collection.
For ex: using the following module configuration, a List<Snack> can be injected.
```java
Module snacksModule = binder -> {
  MultiBinder<Snack> multiBinder = binder.multiBinder(Snack.class);
  multiBinder.addBinding().toInstance(new Twix());
  multiBinder.addBinding().toProvider(SnickersProvider.class);
  multiBinder.addBinding().to(Skittles.class);
};

class SnackMachine {
 @Inject
 public SnackMachine(List<Snack> snacks) { ... }
}
```

If desired, ```Collection<Provider<Snack>>``` can also be injected. 

Contributing multiBindings from different modules is also supported. For example, both CandyModule and ChipsModule can create their own ```MultiBinder<Snack>``` and contribute bindings to the list of snacks. When that list is injected, it will contain elements from both modules.

The injected list is unmodifiable and elements can only be added to the list by configuring the multiBinder. Elements can not be removed from the list.

Annotations can be used to create different lists of the same element type. Each distinct annotation gets its own independent collection of elements.

##### Additional Features

* **@Nullable support**: By default, if an ```@Inject``` annotated dependency (field or parameter), is not present in 
configured binding, a ```NoSuchBindingException``` will be thrown.  If ```null``` value is to be allowed, then 
the field or parameter should be annotated with ```@Nullable```. Injector recognizes any @Nullable annotation 
(by it's simple name), like edu.umd.cs.findbugs.annotations.Nullable or javax.annotation.Nullable. For ex:
    ```java
    public class Computer {
        private final Soundcard soundcard;
    
        @Inject
        public Computer(@Nullable SoundCard soundcard) {
            this.soundcard = soundcard == null ? new Soundcard("basic_sound_card") : soundcard;
        }
    }
    ```
* **Optional<T> support**: A dependency can be declared as ```Optional<T>``` if a binding is not required to be 
configured. An optional Provider dependency can also be injected, like: ```Optional<Provider<Soundcard>>```. 
If there is a binding defined in the module, the Optional value will be present; if there is no binding defined, 
then the Optional will be absent. For ex:
    ```java
    public class Computer {
      private final Soundcard soundcard;
    
      @Inject
      public Computer(Optional<Soundcard> soundcard) {
        this.soundcard = soundcard.orElse(new Soundcard("basic_sound_card"));  
        //this.soundcard = soundcard.isPresent() ? soundcard.get() : new Soundcard("basic_sound_card");
      }
    }
    ```
    And optional dependencies can also be retrieved from injector using one of the following two api:
    ```injector.getIfPresent(type)``` or ```injector.getIfPresent(key)```

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
          final String key = bindingKey.toString();
          return () -> {
              HttpSession session = threadLocal.get();
              if (session == null) {
                  throw new IllegalStateException("Session is not open in this scope, for key:" + key);
              }
              synchronized (session) {
                  Object obj = session.getAttribute(key);
                  if (NullObject.INSTANCE == obj) {
                      return null;
                  }
                  @SuppressWarnings("unchecked")
                  T t = (T) obj;
                  if (t == null) {
                      t = creator.get();
                      session.setAttribute(key, (t != null) ? t : NullObject.INSTANCE);
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

##### InjectionPoint Loader
This is an interface for loading injection points (fields and methods/constructor) created by scanning through target types specified in bindings. It also provides an utility method to create injection points by reflectively scanning through target types. The default internal implementation stores these injection points in a ConcurrentHashMap.

* Custom Injection Point Loader:
 Custom implementations can be created to use an advanced backing cache to store these injection points. Following is a sample custom injection point loader that uses [Caffeine](https://github.com/ben-manes/caffeine) for caching injection points.
  ```java
  public class CaffeineInjectionPointLoader implements InjectionPointLoader {
    private final Cache<Class<?>, List<InjectionPoint<Object>>> cache;

    public CaffeineInjectionPointLoader() {
      this.cache = Caffeine.newBuilder().weakValues().build();
    }

    @Override
    public List<InjectionPoint<Object>> getInjectionPoints(Class<?> clazz,
        Function<Class<?>, List<InjectionPoint<Object>>> creator) {
      return cache.get(clazz, creator);
    }

    @Override
    public void invalidateAll() {
      cache.invalidateAll();
    }
  }
  ```
  And when creating the injector, this custom loader should be used as: 
  ```java
     CaffeineInjectionPointLoader injectionPointLoader = new CaffeineInjectionPointLoader();
     Iterable<Module> modules = ...
     Injector injector = Injectors.create(injectionPointLoader, modules);
  ```

##### Events
```EventBus``` allows publish-subscribe style communication between components, managed by the injector, without requiring them to explicitly register with one another (i.e. no compile-time dependency is required between them).

* Configuring with EventBus Module
When creating the injector, the event bus module should be included as shown below. It binds the EventBus type in ```Singleton``` scope and adds a ```BindingListener``` to find all observer methods.
```java
Injector injector = Injectors.create(EventBus.createModule(), otherModules);
```

* Publishing events
The event bus allows publishing any object as an event. It dispatches the given event object to all observers using ```perThreadDispatchQueue```. It queues events that are posted reentrantly on a thread that is already dispatching an event, guaranteeing that all events posted on a single thread are dispatched to all observers in the order they are posted.
All matching observers are notified of this event on the same thread that posts the event. This yields a breadth-first dispatch order on each thread, i.e. all observers of a single event A will be called before any observers of any events B and C that are posted to the event bus by the observers to A.
```java
@Inject
private EventBus eventBus;

private void createOrder(Order order) {
    eventBus.publish(new OrderCreatedEvent(order));
}
```

* Observing events
An observer method acts as event consumer, by observing events of a specific type. This method will be notified of an event if the event object is matching to the observed event type.

```@Observes``` annotation marks a method as an event observer. The type of event will be indicated by the method's first (and only) parameter. If this annotation is applied to methods with zero parameters, or more than one parameter, the object containing the method will not be able to register for event delivery from the ```EventBus```.
```java
@Observes
private void onOrderCreatedEvent(OrderCreatedEvent event) {
    Order order = event.getOrder();
}
```

* Closing the EventBus
All registered observer methods can be be cleared from internal cache using the close method. This should typically be called, before the injector itself is being disposed.
```java
injector.getInstance(EventBus.class).close();
```

Other JSR-330 spec Implementations
---------------------------------
The following projects, implmenting this specification, are widely used: 
* [Guice](https://github.com/google/guice) (pronounced 'juice') is a lightweight dependency injection framework. And this is also the reference implementation of JSR-330 spec.
* [Spring Framework](https://github.com/spring-projects/spring-framework) provides a comprehensive programming and configuration model for modern Java-based enterprise applications.

And there are also many other implementations available, like:
* [Dagger](https://github.com/google/dagger) is A fast dependency injector for Android and Java.
* [HK2](https://github.com/javaee/hk2) is a light-weight and dynamic dependency injection framework.
* [Commons Inject](https://commons.apache.org/sandbox/commons-inject): This project is not active anymore, but some of Pustike Inject's APIs were initially derived from here.

License
-------
This library is published under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
```
 Copyright (C) 2016-2017 the original author or authors.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
```

[GitHub Actions Status]:https://github.com/pustike/pustike-inject/workflows/Java%20CI/badge.svg

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-inject
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-inject/badge.svg

[Javadocs]:https://javadoc.io/doc/io.github.pustike/pustike-inject
[Javadocs img]:https://javadoc.io/badge/io.github.pustike/pustike-inject.svg

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
