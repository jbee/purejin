# Release notes


v0.9
===============

> Release 0.9 contained no new features but renamed exceptions and "getters" for
> better readability. 

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

- removed `Type#getRawType` (use field access)
- removed `Type#isFinal` (not used)
- removed `Type#elementType(Class)` (not used)
- removed `Bindings#getInspector` (use field access)
- removed `Bindings#getMacros` (use field access)

- declared `InconsistentBinding` as `RuntimeException` 
  (does not extend `UnresolvableDependency` any longer)
- declared `UnresolvableDependency` as `abstract` (should not be instantiated)

- fixed bug in `Type#supertypes()` that added `Object` for interface instead of 
  classes
- changed `Type#elementType` to `baseType` (returns non-array type)
- changed `autobind` expansion to occur via macros


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



**Changes since 0.7**

! = API change!
D = deleted
N = renamed
P = moved package and/or containing class
M = changed/modified

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


