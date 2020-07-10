@ECHO OFF
java --module-path .bach/lib --add-modules de.sormuras.bach .bach/src/build/build/Build.java %*
