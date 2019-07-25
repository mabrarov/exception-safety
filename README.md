# Exception Safety

Java library to support [exception safety](https://en.wikipedia.org/wiki/Exception_safety) ([Wikipedia](https://wikipedia.org)):

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