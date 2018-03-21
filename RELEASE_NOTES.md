# Release notes

v0.11 (upcoming)
==============

- added `initbind` and `Initialiser`s (run code when container is ready)
- macros are now replaced when defined for same type (no behaviour change)
- all value objects in `se.jbee.inject` that implement `equalTo` also
  implement `equals` and `hashCode` (for convenience, no library requirement)
- fixed: `Type` `equals`/`equalTo` now considers `upperBound` flag
- moved `Invoke` static methods to `Supply`
- `PresetModule` now also supplies the `Presets` itself
- supplied constant are now expanded via macros as `BoundConstant`
- explicit `BoundConstant`s implicitly bind the constant actual type too
- fixed: wild-card array dependencies are honored in presents of same raw type bounds
- added `to` for JDK 8 `Supplier` (method references)
- added `autoconnect` to (automatic type bound linkage between instances)

v0.10
==============

> Support for wild-card bindings (supply any sub-class of X).

- improved `DefaultInjector` (now resolving falls back to wild-card bindings)
- moved macro `expand` utility method to `Bindings` (less argument passing)
- renamed `ModularBundle` to `OptionBundle`
- renamed `ModularBootstrapper` to `OptionBootstrapper`
- renamed `ModularBootstrapperBundle` to `OptionBootstrapperBundle`
- renamed `Macros.EMPTY` to `Macros.NONE`
- added `Type#toSupertype` for type compatibility checks
- type compatibility checks (more) consistently throw `ClassCastException`s
- constructor lookup for interface types now throws a `NoMethodForDependency`
- action execution now wraps all runtime exceptions in `ActionMalfunction`
 

v0.9
===============

> Release 0.9 contained no new features but renamed exceptions and "getters" for
> better readability. Auto-binding implementation moved to macros. 
> Providers utilise injectrons for better performance. 
> Injectrons now forward the dependency's name
> Services got renamed (and repackaged) as actions

Exceptions got renamed to better reflect the problem (the generic term 
`Exception` is no longer necessary). Interfaces explicitly state the exceptions
thrown (even though these are not checked exceptions).

- renamed `DIRuntimeException` to `UnresolvableDependency`
- renamed `DependencyCycleException` to `DependencyCycle`
- renamed `NoSuchResourceException` to `NoResourceForDependency`
- renamed `NoSuchFunctionException` to `NoMethodForDependency`
- renamed `MoreFrequentExpiryException` to `UnstableDependency`
- renamed `BootstrappingException` to `InconsistentBinding`
- renamed `Typed#getType` to `type`
- renamed `Resource#getName` to `name`
- renamed `Injectron#getInfo` to `info`
- renamed `Dependency#getName` to `name`
- renamed `Dependency#getInstance` to `instance`
- renamed getters in `Assembly` to not use `get` prefix
- renamed getters in `Bindings` to not use `get` prefix
- renamed `Bind#getInspector` to `inspector`
- renamed `Type#getArrayType` to `addArrayDimension`
- renamed `Type#getParameters` to `parameters` (does a clone!)
- renamed `Type#allArgumentsAreUpperBounds` to `areAllTypeParametersAreUpperBounds`
- renamed `DeclarationType#nullifiedBy` to `droppedWith`
- renamed `Factory#produce` to `fabricate` (less confusing naming)
- renamed `ServiceMethod` to `Action` (also moved to `procedure` package)
- renamed `ServiceMethod#invoke` to `Action#exec`
- renamed `ServiceMalfunction` to `ActionMalfunction`
- renamed `ServiceModule` to `ActionModule`
- renamed `ServiceModule#bindServiceMethodsIn` to `bindActionsIn`
- renamed `ServiceModule#serviceDependency` to `actionDependency`
- renamed `ServiceModule#SERVICE_INSPECTOR` to `ACTION_INSPECTOR`
- renamed `ServiceModule#bindServiceInspectorTo` to `discoverActionBy`

- removed `UnresolvableDependency#injectionStack` (now `Dependency`'s `toString`)
- removed `Type#getRawType` (use field access)
- removed `Type#isFinal` (not used)
- removed `Type#elementType(Class)` (not used)
- removed `Bindings#getInspector` (use field access)
- removed `Bindings#getMacros` (use field access)
- removed `Dependency#anyTyped` (use `instanced(anyOf(...))` instead)
- removed `Dependency#instance` (use field access)
- removed `Dependency#name` (use `instance.name` instead)
- removed `Resource#name` (not needed, use field access)
- removed `ServiceModule#bindInvocationHandler` (no core feature, see test)
- removed `ServiceInvocation` (became test example of how to add such a thing)
- removed `ServiceProvider` interface (rebind `Action` instead, adapt via
  `ActionModule.actionDependency`)

- declared `UnresolvableDependency` as `abstract` (should not be instantiated)
- declared `InconsistentBinding` as `RuntimeException` 
  (does not extend `UnresolvableDependency` any longer)

- fixed `Type#supertypes()` that added `Object` for interfaces not classes

- changed `Type#elementType` to `baseType` (returns non-array type)
- changed `autobind` expansion to occur via macros
- changed `Dependency#onTypeParameter` keeps instance name
- changed semantics of resolving `Injectron`s (cannot be bound any more)
- changed `Action` explicitly throws new `ActionMalfunction` exception
- changed unified exception handling when invoking methods and constructors

- improved `Provider` implementation to use `Injectron`s (better performance!)
- improved `Injectorn` dependencies forward `Name`

- added `Dependency#equalTo` (and `equals`)
- added `InjectionSite` utility to provide "cached" arguments
- added `SupplyFailed` exception (errors during supply attempt)


v0.8
===============

> Release 0.8 contained no particular new feature but a simplification of inner
> mechanics.

The Binder API usually used by library users has minimal changes that are 
**marked bold**. 
The `service` facility and the `Macro` expansion as well as the `Repository` 
abstraction have been redesigned. 
The container implementation details have been moved to a new package 
`container`. The data types have been cleaned up, field access now is the 
preferred way to read immutable values.
The type of thrown exception has been revised in favour of more specific 
exceptions and stated contracts have been added where possible. The naming of
mostly internal types, fields and methods has been improved. 

- **deleted** `configbind` (see `TestStateDependentBinds` for replacement)
- deleted `Configuring` (no core feature any more as it is easy to add)
- deleted `Value` (API redesigned)
- deleted `InjectronSource` (`Injector` is created from `Binding`s directly)
- deleted `Resourced` (not needed any more)
- deleted `Injectable` (API redesigned)
- deleted `Linker`, `Link` and `Suppliable` (became part of `Inject`)
- deleted `Precision` (methods moved)
- deleted `Named` (not needed any more)
- deleted `Emergence` (API redesign)
- deleted `Demand` (see `InjectronInfo`)
- deleted `ToString` (methods moved to `SuppliedBy`)
- deleted `Parameterization`,`Parameterize` (replaced with `BoundParameter`)
- deleted `implicitBindToConstructor` (obsolete; done via macros)
- deleted `Naming` (use `String` or values with proper `toString`)
- deleted `Extension` (use `plug`+`into` or `bindServiceMethodsIn` instead)
- deleted `ExtensionModule`, `ServiceClassExtension` (see above)
- deleted `ServiceInvocationExtension` (use `bindInvocationHandler`)
- deleted `Name#namedInternal` (no internal names any more)
- deleted `MacroModule` (was unused and a wrong idea)

- renamed `PreciserThan` to `MorePreciseThan`
- renamed `Producible` to `BoundMethod` and moved to `bootstrap` package
- renamed `KeyDeduction` to `DependencyProperty`
- renamed `SuppliedBy#resolve` to `resolveParameters`, `resolveParameter`
- renamed `Constructible` to `BoundConstructor` (moved into `bootstrap` package)
- renamed `SuppliedBy` to `Supply` and moved it into `bootstrap` package
- renamed `Binding#suppliedBy` to `Binding#complete`
- renamed `isApplicableFor` to `isCompatibleWith`
- renamed `Bindings.expand` to `declareFrom`
- renamed `BindingType#SUBSTITUTED` to `LINK`
- renamed `Macros#use` to `with`
- renamed `Macros#FORWARD` to `PARAMETRIZED_LINK`
- renamed `Macros#SUBSTITUTE` to `INSTANCE_LINK`
- renamed `Macros#PRODUCE` to `FACTORY_METHOD`
- renamed `Macros#CONSTRUCT` to `FACTORY_METHOD`

- moved `Metaclass` to `bootstrap`
- moved methods from `Precision` to `Instance`
- moved `Inject`, `Scope`, `Scoped`, `Repository`, `Provider`, `Factory`, 
  `Typecast` to new `container` package

- redesigned `Repository` interface
- redesigned `Injectron` interface (see `InjectronInfo`)
- redesigned `Macro` interface changed completely

- redefined `Injector` to return an empty array if no bind existed for element 
  type instead of throwing an exception
- redefined `Typed#typed` now explicitly has the contract to throw 
  `ClassCastException` when argument type is not valid


Prior v0.8
===============
No release notes available.


