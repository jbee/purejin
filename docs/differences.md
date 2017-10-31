---
layout: default
---

# A Different Container

Let me try to explain why silk DI is not similar to (most) other containers like google guice, dagger or spring DI and why it matters.

## The Fancy Map Model

Lets imagine a very simple container as a map. 
We "declare" an instance entry in that map by "attaching" a "unique" instance name to a canonical class name.
We "resolve" that instance by attaching the same name to a field or parameter to "tell" which instance we want.  
This could be called "resolution by name". Quit simple.
This is more or less how spring started.

```
"foo" => "my.package.Foo"
```

Lets add a little convenience feature: Classes with a certain feature (like package or an annotation) are "added" to the container without stating something explicitly.
We would add multiple entries to the map. One for the exact canonical name of the class (as instance name) and one for the names of each superclass or interface (except unreasonable ones like `Object`). 
Also when "resolving" an instance without stating a name we automatically look for the instance with the target type name.
This could be called "resolution by type". 
Spring later added it. In guice this is the "usual" way to resolve dependencies.
This, however, does constraint us to one instance per type. Too simplistic.

So, lets tweak our feature a little bit. When a name is used in connection with a type this is meant as a type "local" name instead of a map "global" name. In practice we can imagine this to work like before; except that we add even more entries to the map where we combine the "local" name with the class name(s) to compute further names for our map
that all lead to the very same instance. 
We can imagine that guice's `@Named` works like that. 

One thing got overlooked when we added "types" to the names. 
Multiple types can have the same supertype. 
Like multiple classes implementing the same interface.
This has to work smothly. 
So our map's values really have to be lists of classes associated with a name.
But if we actually try to resolve a name that is associated with multiple classes/instances we don't know which one we should use and have to throw an error.
These errors should be familiar from guice and spring.
They are necessary because of the map-model. 

While we can understand how we got to what I call the "fancy map model"<sup>[1](#fn1)</sup> it requires some thought to understand why it is a poor solution.
   

## Abstraction and Composition

TODO

* DI => more general app wiring
* Ask wrong question: Ask the receiver: What instance do you want to receive?
* Breaks abstraction and composition: the two things the framework should have helped with
* Receiver should not need to know (abstraction) and must be able to receive different (composition)

-------
1. <a name="fn1"></a>I'm not saying anyone implemented it this way. It is just a simplified mental model for how we can imagine it to work. 

