---
layout: default
---

# How it works
At the core is a quite simple instance container. 
It consists of a list of injectrons sorted by precision (most precise first) 
that is assembled from bindings during initialization and then never changes again.
Each injectron is a factory like typed source of instances. When a dependency is resolved
the first injectorn that matches the requested dependency is asked to yield
the instance for it. If the requested type is a 1-dimensional array and no
injectorn specifically for that type exists, the resulting array is assembled
from all matching element instance providers. That's it.

## Utilities
Most of the library is just a utility build around the container.
The fluent API is a utility to describe bindings (assemblies) to create a
container from. Modules and bundles are utilities to organize and assemble
sets of such bindings to allow configuration and customization during the
bootstrapping of a container. 
Suppliers, repositories and scopes are abstractions to create and manage 
instances and their life-cycles. Providers and factories make this even more
convenient. Finally actions utilize several features to build a 
input-processing-output abstraction onto methods.

## Precision?
Both dependencies (instance requirements) as well as injectrons (instance providers)
are (also) data descriptions of what they require/provide.
Such descriptions can be more or less specific or precise.
A wild-card type is less precise than a fully specified one.
A sub-class more specific than a super-class.
A named instance more precise than a unnamed one, 
one specifically meant for a particular package or parent instance is again more 
precise than one that doesn't specify these. 
There is no particular logic other than: the more we know about when/where
something applies the more specific or precise it is. 
All in all it is fairly intuitive - one just needs to remember the sequence 
in which properties are compared: type, name, hierarchy, package.

