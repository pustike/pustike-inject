Pustike Inject Changes
======================

Release v1.0.1 (2017-04-30)
--------------------------
* Provider Bindings: Inject dependencies into provider instance only once.
* Fix Scoped Provider creation issue in a multi-threaded environment by creating it immediately after injector configuration.
* Singleton scope: define instance variable as ```volatile``` to avoid issues in a multi-threaded environment (derived from Guice).
* Singleton scope: Call provider.get() only once even when it returns the instance as null.
* Include Field/Method details in the exception thrown, when failed to inject dependencies.
* Make utils\AnnotationUtils package-private as it contains internal utility methods only.
* Upgrade build to Gradle v3.5

Release v1.0.0 (2017-03-07)
--------------------------
* Initial Public Release
