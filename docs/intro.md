
Let me try to explain why silk DI is not similar to (most) other frameworks like google guice, dagger or spring DI and why it matters.

Lets imagine a very simple container as a map. 
We "declare" an instance entry in that map by "attaching" a name to a class.
We "resolve" that instance by attaching the same name to a field or parameter to "tell" which instance we want.  
This could be called "resolution by name". Quit simple.
This is more or less how spring started.

Lets add a little convenience feature: A class can be "added" to the container without naming it explicitly.
Than we would add multiple entries to the map. One for the exact canonical name of the class and one for the names of each superclass or interface (except unreasonable ones like `Object`). 
Also when "resolving" an instance without stating a name we automatically look for the instance with the target type name.
This could be called "resolution by type". 
Spring later added it. In guice this is the "usual" way to resolve dependencies.
This, however, does constraint us to one instance per type. Too simplistic.

So, lets tweak our feature a little bit. When a name is used in connection with a type this is meant as a type "local" name instead of a map "global" name. In practice we can imagine this to work like before; except that we add even more entries to the map where we combine the "local" name with the class name(s) to compute further names for our map
that all lead to the very same instance. 
We can imagine that guice's `@Named` works like that. 

One thing got overlooked when we added "types" to the names. 
Multiple types can have the same supertype. This should not cause trouble. 
Different "services" might inherit from an "abstract" service.
Still, we would never try to resolve the abstract service.
So the map values in general are lists of classes.
But if we actually try to resolve a name that is associated with multiple classes/instances we don't know which one we should use and have to throw an error.
These errors should be familiar from guice and spring.
They are necessary because of the map-model. 

While we can understand how we got to this model it requires some thought to understand why it is a poor solution.
   



....
DI => more general app wiring
Ask wrong question: Ask the receiver: What instance do you want to receive?
Breaks abstraction and composition: the two things the framework should have helped with
Receiver should not need to know (abstraction) and must be able to receive different (composition)
