Pustike Inject Changes
======================

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
