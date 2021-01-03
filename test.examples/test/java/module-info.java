import test.ExamplesBundle;
import test.ExamplesEnvBundle;
import test.example1.SupportAnnotationTemplet;

/**
 * Contains an example application used in tests to test the {@link
 * java.util.ServiceLoader} based features.
 */
@SuppressWarnings("Java9RedundantRequiresStatement")
open module test.examples {

  exports test;
  exports test.example1;
  exports test.example2;

  requires transitive se.jbee.inject;

  provides se.jbee.inject.bind.Bundle with ExamplesBundle, ExamplesEnvBundle;

  provides se.jbee.inject.bind.ModuleWith with SupportAnnotationTemplet;
}
