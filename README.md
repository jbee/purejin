## silk

### Java Dependency Injection

#### 100%
- **Configuration in code**
- **beside your application code** (no dependency within it)

#### Less _Magic_
- no AOP
- no accessible mutable state
- no reconfiguration 
- no annotations (easy to add but why would you want to ?)

#### Modular
- powerful modularisation with bundles and modules
- allows: repeated install of bundles (also circular), uninstall bundles
- if-less configuration through editions, features and constant-properties

#### Further Characteristics
- data driven (core uses a lot Value Objects)
- pushes for immutability: enforces Constructor-Injection
- multi-binds
- allows use of generics in managed instances without pain
- typesafe (not fully typesafe with generics)
- easy to extend (e.g. with Set or List support)
- decouples application layers through services
- Unit-testable 
- has a guice like binding-builder
- and yes: it's pretty fast too

#### Status
The project is in a late conception phase. Most of the core concepts are implemented and will just get minor adjustments. But a few are missing. As long as those are under development there will be no examples but you can always look at the tests (the ones in the `bind` package demonstarte the features quite nice).

Please come back soon and check for updates. 