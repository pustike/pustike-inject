Pustike Inject Changes
======================

Release v1.4.2 (2018-07-18)
--------------------------
* Fields and Methods are scanned for injection points, by sorting them using their name and types.
* Fixed: Declaring a binding as Eager Singleton should create the instance when injector is configured.
* Other general improvements.

Release v1.4.1 (2018-04-10)
--------------------------
* Improvements and bug fixes.

Release v1.4.0 (2018-02-23)
--------------------------
* New Feature: Events support to allow publish-subscribe style communication between components,
    managed by the injector, without requiring them to explicitly register with one another.
* Create a SPI (Service Provider Interfaces) package and moved following interfaces from ```bind``` package:
  ```BindingListener```, ```InjectionListener```, ```InjectionPoint```, ```InjectionPointLoader```.
* ```BindingListener``` is improved to include the ```BindingKey``` for which a binding has been registered.
* ```InjectionPointLoader``` changes:
  - Removed method ```createInjectionPoints``` which had a cyclic dependency with ```impl``` package
  - A function ```injectionPointCreator``` is passed to the method ```getInjectionPoints```
* Fixed a regression introduced in v1.3.0, which ignored ```defaultScope``` applied in the module.
* Added check to ensure that, Bindings can be obtained only after the Injector is fully configured.
* Change ```toString()``` implementation of ```@Named``` annotation to quote string values when using Java 9.

Release v1.3.0 (2017-12-18)
--------------------------
* Added support for optional dependencies using ```@Nullable``` and ```Optional<T>```
* Added new methods to retrieve optional dependencies from injector:
    ```injector.getIfPresent(type)``` and ```injector.getIfPresent(key)```
* Fixed the issue in configuring MultiBinder when bindings are added from different modules for the same key.

Release v1.2.0 (2017-11-14)
--------------------------
* Added **MultiBinder** support to bind multiple values separately, to later inject them as a complete collection. More details are available in javadocs.
* Added methods to ```BindingKey```: ```toListType()``` and ```toListProviderType()```
* Renamed ```BindingKey#createProviderKey()``` method as ```toProviderType()``` 
* Fixed the issue in binding a provider when ```@Provides``` annotation is used on a method in the module
* Scan for methods annotated with ```@Provides``` in module's superclasses also
* Fixed the issue which allowed registering bindings to a key more than once.

Release v1.1.0 (2017-06-30)
--------------------------
* **Hierarchical Injector** support by creating child injector which delegates all requests for bindings that are not found, to it's parent injector. All bindings in the parent injector are visible to child, but elements of the child injector are not visible to its parent. Following new APIs are introduced:
  * ```Injector#createChildInjector(Module...)```
  * ```Injector#createChildInjector(Iterable)```
  * ```Injector##getParent()```
* Added ```Binder.install(Module)``` to configure bindings from the given module.
* Bind Module methods annotated with ```@Provides``` to create provider method bindings.
* Remove ```SessionScope``` from test packages as it belongs to pustike-web module 
* Update build files to Gradle v4.0 release

Release v1.0.1 (2017-04-30)
--------------------------
* Provider Bindings: Inject dependencies into provider instance only once.
* Fix Scoped Provider creation issue in a multi-threaded environment by creating it immediately after injector configuration.
* Singleton scope: define instance variable as ```volatile``` to avoid issues in a multi-threaded environment (derived from Guice).
* Singleton scope: Call ```provider.get()``` only once even when it returns the instance as null.
* Include Field/Method details in the exception thrown, when failed to inject dependencies.
* Make utils\AnnotationUtils package-private as it contains internal utility methods only.
* Upgrade build to Gradle v3.5

Release v1.0.0 (2017-03-07)
--------------------------
* Initial Public Release
