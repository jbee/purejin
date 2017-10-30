---
layout: default
---

# Dependency Injection Through Code.
* java
* dependency injection
* library
* refactoring

## What happened?

The detailed documentation provided earlier -- as so often -- got outdated when the code
evolved. I decided to not explain something in words again that code can say so much better.

The following is not much but it will be accurate. 
If something is not covered [ask me](http://jbee.github.io).

## What it is
Dependency management through code. 
**No XML**. **No annotations**. No code dependencies in the wrong direction. 
Application code is written as if there is no DI library. 
Indeed, this is the goal: wiring up the application had become so simple that no 
sophisticated library is needed to aid it. 
Silk will make sophisticated wiring easier until it is no longer needed and
gracefully disappears as simplicity emerges.

## Why it came to be
The classic struggle: How hard could it be to write something better than 
the so called "mature" traps mostly used in the enterprise? 
As often it turned out: not that complicated.
More importantly: while the major players are all addictive frameworks the silk
library is intended as a substitute that in the end gets rid of itself.
Confused? It's just saying: [Small is beautiful](http://www.infoq.com/presentations/small-large-systems).
We don't need a library for that. So I'm not using it.

## Why use it?
If you have already decided to use a container you're most likely better off 
with silk. It's tiny, debuggable, straight forward stand alone library that 
makes common things easy and uncommon ones easy to add.
It avoids anything not refactoring-safe, confusing, complicated or otherwise 
hard to maintain like XML or annotations, aspects, bytecode rewirting, classloader magic etcetera.
It really is just plain old boring code. 
However, it makes some strong decisions to keep dependency injection sane.
Don't fight them. Ask why. Learn.

Or you are just one of those unlucky employees getting told to mess things up
because business really needs this feature and that. Not that they know what it
does.

## What it can do
Fluent API with:

* basic semantic: bind this to that under those circumstances
* bind types or instances (named instances of a type)
* bind with full generic support (even different binds depending on different generics)
* bind any class/interface (no exceptions or special handling)
* bind multiple values and receive them as array, list, set
* use your list/set implementations instead of javas (is really easy)
* bind different configurations without a single `if`
* bind in different scopes
* use your own scopes/lifecycles (is really easy)
* use your own provider interface to escape scoping problems (is really easy) 
* restrict bindings to packages, types, instances or type/instance hierarchies
* extendable using binds
* ...

The container will tell you at **construction time** what is missing, ambigous or
wrongly scoped. No pesky problems later on.

## How to use it
* Checkout the [sources](https://github.com/jbee/silk) or pick a 
[release](https://github.com/jbee/silk/releases),
* use `ant jar` or `ant release` to build a jar and add it to the classpath,
* declare some bindings in a module using a fluent API
{%highlight java %}
class RobotLegsProblem extends BinderModule {

	@Override
	protected void declare() {
		construct( Robot.class );
		//...
		bind( left, Leg.class ).toConstructor();
		bind( right, Leg.class ).toConstructor();
		injectingInto( left, Leg.class )
			.bind( Foot.class ).to( left, Foot.class );
		injectingInto( right, Leg.class )
			.bind( Foot.class ).to( right, Foot.class );
	}
}
{%endhighlight%}
* bootstrap the container
{%highlight java %}
Injector injector = Bootstrap.injector( RobotLegsProblem.class );
{%endhighlight%}
* resolve the root instance
{%highlight java %}
Robot robot = injector.resolve( dependency( Robot.class ) ); 
{%endhighlight%}

## What about maven?
Maven is part of the complexity problem. I you don't see that yet you will struggle to see
why someone would prefer a library over a framework, code over annotations and
so forth. Take it as a hint. Maybe you feel that something is quite wrong. 
Open your mind for the possibility that maven is one of those things and you'll understand. 

## How to learn it
The [tests](https://github.com/jbee/silk/tree/master/src/test/se/jbee/inject/bind)
for the binding API do illustrate what can be done and how to do it. 
It's all ordinary code. Read it.

Not very handy but it doesn't lie. A tour could be to look at

* [most basic](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestInstanceBinds.java) (wire this to that)
* [constants](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestConstantBinds.java)
* [primitives](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestPrimitiveBinds.java)
* [arrays](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestElementBinds.java)
* [primitive arrays](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestPrimitiveArrayBinds.java)
* [collections](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestCollectionBinds.java)
* [parent dependent binds](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestParentTargetBinds.java)
* [package dependent binds](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestPackageLocalisedBinds.java)
* [auto-binds](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestAutobindBinds.java) (bind supertypes)
* [multi-binds](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestMultibindBinds.java) (bind multiple values)
* [options](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestOptionBinds.java) (or how to not end up with if's)
* [presets](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestPresetModuleBinds.java) (or how not to pass values around in module code)
* [require-provide](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestRequiredProvidedBinds.java) (loose coupling like to OSGi requirements and capabilities)
* [loggers](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestLoggerBinds.java) (example of target specific injection)
* [parameter hinting 1](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestSpecificImplementationBinds.java)
* [robots legs problem](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestRobotLegsProblemBinds.java)
* [parameter hinting 2](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestConstructorParameterBinds.java)
* [providers](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestProviderBinds.java) (think guice provider)
* [plug-ins](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/bind/TestPluginBinds.java) (a basic extension mechanism)
* [actions](https://github.com/jbee/silk/blob/master/src/test/se/jbee/inject/action/TestActionBinds.java) (an extension to wire methods as services)

Looking at the other tests in the same folder will also be useful. 
There is more to discover. Most likely silk allows to do what you want.
Otherwise just ask [me](http://jbee.github.io) for help.

## How it works
At the core is a quite simple instance container. 
It consists of a list of injectrons sorted by precision (most precise first) 
that is assembled from bindings during initialization and then never changes again.
Each injectron is a factory like typed source of instances. When a dependency is resolved
the first injectorn that matches the requested dependency is asked to yield
the instance for it. If the requested type is a 1-dimensional array and no
injectorn specifically for that type exists, the resulting array is assembled
from all matching element instance providers. That's it.

#### Utilities
Most of the library is just a utility build around the container.
The fluent API is a utility to describe bindings (assemblies) to create a
container from. Modules and bundles are utilities to organize and assemble
sets of such bindings to allow configuration and customization during the
bootstrapping of a container. 
Suppliers, repositories and scopes are abstractions to create and manage 
instances and their life-cycles. Providers and factories make this even more
convenient. Finally actions utilize several features to build a 
input-processing-output abstraction onto methods.

#### Precision?
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

