# Exception Safety

[![Release](https://img.shields.io/github/release/mabrarov/exception-safety.svg)](https://github.com/mabrarov/exception-safety/releases/latest) 
[![License](https://img.shields.io/github/license/mabrarov/exception-safety)](https://github.com/mabrarov/exception-safety/tree/master/LICENSE)
[![Travis CI build status](https://travis-ci.com/mabrarov/exception-safety.svg?branch=master)](https://travis-ci.com/mabrarov/exception-safety)
[![AppVeyor CI build status](https://ci.appveyor.com/api/projects/status/t90klsr0etvfutqm/branch/master?svg=true)](https://ci.appveyor.com/project/mabrarov/exception-safety/branch/master)
[![Code coverage status](https://codecov.io/gh/mabrarov/exception-safety/branch/master/graph/badge.svg)](https://codecov.io/gh/mabrarov/exception-safety/branch/master)
[![Black Duck Security Risk](https://copilot.blackducksoftware.com/github/repos/mabrarov/exception-safety/branches/master/badge-risk.svg)](https://copilot.blackducksoftware.com/github/repos/mabrarov/exception-safety/branches/master)

Java library to support [exception safety](https://en.wikipedia.org/wiki/Exception_safety) (from [Wikipedia](https://wikipedia.org)):

> 1. **No-throw guarantee**, also known as **failure transparency**: 
>    Operations are guaranteed to succeed and satisfy all requirements even in exceptional situations.
>    If an exception occurs, it will be handled internally and not observed by clients.
> 1. **Strong exception safety**, also known as **commit or rollback semantics**: 
>     Operations can fail, but failed operations are guaranteed to have no side effects, 
>     leaving the original values intact.
> 1. **Basic exception safety**, also known as a **no-leak guarantee**: 
>    Partial execution of failed operations can result in side effects, 
>    but all invariants are preserved and there are no resource leaks (including memory leaks). 
>    Any stored data will contain valid values which may differ from the original values.
> 1. **No exception safety**: No guarantees are made.

Providing classes which implement first 2 exception safety guarantees helps to create code which 
provides no-leak guarantee and so prevents resource leaks which is still possible in Java when 
"resource" means not just heap memory (like `java.io.OutputStream`).

To build types providing exception safety guarantee like no-throw guarantee or strong exception safety, 
below Java operations / statements are considered providing no-throw guarantee and are used 
as building blocks:

1. assignment operator, i.e. assignment of reference or primitive type including assigment operator 
   in "try-with-resources" statement
1. reading / returning reference or primitive type
1. arithmetic operations within correct range / with correct parameters supported by primitive type
1. invocation of method which provides no-throw guarantee
1. `java.lang.System#arraycopy` method when correct parameters are given
1. `java.util.ArrayList#remove(int index)` method when correct `index` is given, 
   because this method is implemented only with operations described above
1. `java.util.ArrayList#get(int index)` method when correct `index` is given, 
   because this method is implemented only with operations described above
1. `java.util.ArrayList#set(int index, E element)` method when correct `index` is given, 
   because this method is implemented only with operations described above
1. `java.util.ArrayList#size()` method, because this method is implemented only with operations 
   described above

## Building

### Requirements

1. JDK 1.7+

## Steps

Build with [Maven Wrapper](https://github.com/takari/maven-wrapper):

```bash
./mvnw clean package
```

or on Windows:

```cmd
mvnw.cmd clean package
```

## Usage

### Example 1

Constructor providing basic exception safety.

```java
package bar;

import org.mabrarov.exceptionsafety.Guard;

public class Foo implements AutoCloseable {

  private static AutoCloseable createResource1() throws Exception {
    // Init / open and return some resource, may throw exception.
    // This method exists just for simplification of reading.
  }

  private static AutoCloseable createResource2() throws Exception {
    // Init / open and return some resource, may throw exception.
    // This method exists just for simplification of reading.
  }

  private static AutoCloseable createResource3() throws Exception {
    // Init / open and return some resource, may throw exception.
    // This method exists just for simplification of reading.
  }

  private static void doSomeInitialization() throws Exception {
    // Perform some initialization which may throw exception.
    // This method exists just for simplification of reading.
  }

  private AutoCloseable resource1;
  private AutoCloseable resource2;
  private AutoCloseable resource3;

  // Provides no-leak guarantee
  public Foo() throws Exception {
    // Create guard for particular resource (instance of AutoCloseable) before resource is created
    // to avoid OOM (when creating guard) causing leak of resource.
    try (Guard guard1 = new Guard(); 
         Guard guard2 = new Guard(); 
         Guard guard3 = new Guard()) {
      // Guard resource right after it was created.
      // Guard#set(AutoCloseable) method provides no-throw guarantee 
      // so there cannot be resource leak.
      resource1 = guard1.set(createResource1());

      // If Foo#createResource2() method throws exception 
      // then resource1 is closed by guard1 and "try-with-resource" statement.
      // If exception happens when closing resource1 then that exception is nested 
      // (suppressed - refer to Throwable#getSuppressed() method) by "try-with-resource" statement
      // so no exception is lost or overridden.
      resource2 = guard2.set(createResource2());

      // If Foo#createResource3() method throws exception 
      // then resource1 and resource2 are guaranteed closed.
      resource3 = guard3.set(createResource3());

      // If Foo#doSomeInitialization() method throws exception 
      // then resource1, resource2 and resource3 are guaranteed closed.
      doSomeInitialization();

      // No more initialization code which may throw exception
      // so it's safe to release all guards.
      
      // Below methods provide no-throw guarantee.
      guard3.release();
      guard2.release();
      guard1.release();
    }
  }

  @Override
  public void close() throws Exception {
    // Use trick with "try-with-resources" for guaranteed closure of resources and correct nesting
    // of exception (instead of losing some of subsequent exceptions).
    try (@SuppressWarnings("unused") AutoCloseable closer1 = resource1;
         @SuppressWarnings("unused") AutoCloseable closer2 = resource2) {
      resource3.close();
    }
  }
}
```

### Example 2

Constructor providing basic exception safety and ability to call `close` method multiple times 
without calling `close` method of underlying resources multiple times if they were closed 
successfully, i.e. if their `close` method did not throw exception. Subsequent calls of 
`close` method of `Foo` class call `close` method of underlying resources only for resources which 
were closed with error (`close` method thrown exception) during previous call, i.e. only for 
underlying resources which cannot be considered as guaranteed closed.

This example is modified example #1, so some comments which exist in example #1 are omitted.

```java
package bar;

import org.mabrarov.exceptionsafety.Guard;

public class Foo implements AutoCloseable {

  private static AutoCloseable createResource1() throws Exception {
    // ...
  }

  private static AutoCloseable createResource2() throws Exception {
    // ...
  }

  private static AutoCloseable createResource3() throws Exception {
    // ...
  }

  private static void doSomeInitialization() throws Exception {
    // ...
  }

  // Use Guard and not AutoCloseable to support subsequent calls of Foo#close().
  // Refer to comments in Foo#close() method for details.
  private Guard resource1 = new Guard();
  private Guard resource2 = new Guard();
  private Guard resource3 = new Guard();

  // Provides no-leak guarantee
  public Foo() throws Exception {
    try (Guard guard1 = new Guard(); 
         Guard guard2 = new Guard(); 
         Guard guard3 = new Guard()) {
      AutoCloseable resource1 = guard1.set(createResource1());
      AutoCloseable resource2 = guard2.set(createResource2());
      AutoCloseable resource3 = guard3.set(createResource3());

      doSomeInitialization();

      // No more initialization code which may throw exception,
      // so it's safe to release all guards.
      
      // Below methods provide no-throw guarantee.
      // Move resources from local scope of current method to the members of Foo instance.
      this.resource3.swap(guard3);
      this.resource2.swap(guard2);
      this.resource1.swap(guard1);
      // guard1, guard2 and guard3 are empty here because they were swapped with 
      // empty instances of Guard. This means that starting from this line Guard#close() method for
      // guard1, guard2 and guard3 does nothing and ownership of resources was moved from 
      // local scope to created instance of Foo class.
    }
  }

  @Override
  public void close() throws Exception {
    // Note that if some resource throws exception during call of its AutoCloseable#close() method
    // then caller of Foo#close() method is able to call it one more time - subsequent calls
    // close only resources which were not closed successfully (without exception) during 
    // previous calls.
    try (@SuppressWarnings("unused") AutoCloseable closer1 = resource1;
         @SuppressWarnings("unused") AutoCloseable closer2 = resource2) {
      resource3.close();
    }
  }
}
```

### Example 3

Guard resource to prevent resource leak in factory method.

```java
package bar;

import org.mabrarov.exceptionsafety.Guard;

public class Foo {
  
  public static class Resource implements AutoCloseable {
    // some logic
  } 

  public void doSomethingUsingResource() throws Exception {
    try (Resource resource = createResource()) {
      // do something with resource
    }
  }

  // Provides no-leak guarantee
  private Resource createResource() throws Exception {
    // Create guard before resource to avoid OOM (when creating guard) causing leak of resource.
    try (Guard guard = new Guard()) {
      Resource resource = guard.set(new Resource());
      
      // Some logic which may throw exception goes here.
      // If this call throws exception then resource is closed by guard.
      configureResource(resource);
 
      // Below method provides no-throw guarantee.
      // Guard#close() does nothing when below statement completes.
      guard.release();
      return resource;
    }
  }

  private void configureResource(Resource resource) throws Exception {
    // Configure "opened" instance of resource, may throw exception.
    // This method exists just for simplification of reading.
  }
}
```

### Example 4

Example #2 simplified by usage of `NestedGuard` class.

```java
package bar;

import org.mabrarov.exceptionsafety.Guard;

public class Foo implements AutoCloseable {

  private static AutoCloseable createResource1() throws Exception {
    // ...
  }

  private static AutoCloseable createResource2() throws Exception {
    // ...
  }

  private static AutoCloseable createResource3() throws Exception {
    // ...
  }

  private static void doSomeInitialization() throws Exception {
    // ...
  }

  // Use NestedGuard to simplify closing of multiples instances of AutoCloseable.
  private NestedGuard guard = new NestedGuard();

  // Provides no-leak guarantee
  public Foo() throws Exception {
    try (NestedGuard localGuard = new NestedGuard()) {
      // If localGuard.add fails (for example due to OOM) then it closes resource created by
      // createResource1() before throwing exception, so resource doesn't leak.
      // If close() method of resource throws exception too, then that exception is nested as 
      // suppressed by original exception thrown by localGuard.add method.
      AutoCloseable resource1 = localGuard.add(createResource1());

      AutoCloseable resource2 = localGuard.add(createResource2());
      AutoCloseable resource3 = localGuard.add(createResource3());

      doSomeInitialization();

      // No more initialization code which may throw exception,
      // so it's safe to release localGuard.
      
      // Below method provides no-throw guarantee.
      // Move resources from local scope of current method to the member of Foo instance.
      guard.swap(localGuard);
    }
  }

  @Override
  public void close() throws Exception {
    // Closes resource3, then resource2, then resource1.
    // If some resource throws exception when its AutoCloseable#close method is called, 
    // then that resource remains in guard so subsequent call of Foo#close method still 
    // closes that resource.
    // If resource is closed successfully (without exception) then it is removed from guard so that
    // subsequent calls of Foo#close method don't impact that resource (as well as resource may be
    // collected by GC because it's not referenced by guard).
    guard.close();
  }
}
```
