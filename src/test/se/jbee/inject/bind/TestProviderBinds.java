package se.jbee.inject.bind;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.util.Typecast.providerTypeOf;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import se.jbee.inject.DIRuntimeException.MoreFrequentExpiryException;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.util.Provider;
import se.jbee.inject.util.Scoped;

public class TestProviderBinds {

	static final DynamicState DYNAMIC_STATE_IN_OUTER = new DynamicState();

	private static class ProviderBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "foobar" );
			bind( CharSequence.class ).to( "bar" );
			bind( Integer.class ).to( 42 );
			bind( named( "foo" ), Integer.class ).to( 846 );
			bind( Float.class ).to( 42.0f );
			per( Scoped.INJECTION ).bind( DynamicState.class ).toConstructor();
			construct( FaultyStateConsumer.class );
			construct( WorkingStateConsumer.class );
			injectingInto( WorkingStateConsumer.class ).within( OuterStateConsumer.class ).bind(
					DynamicState.class ).to( DYNAMIC_STATE_IN_OUTER );
			construct( OuterStateConsumer.class );
		}

	}

	private static class ProviderBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			installAll( BuildinBundle.class );
			install( ProviderBindsModule.class );
		}

	}

	private static class DynamicState {

		DynamicState() {
			// this is constructed per injection so it is "changing over time"
		}
	}

	private static class FaultyStateConsumer {

		@SuppressWarnings ( "unused" )
		FaultyStateConsumer( DynamicState state ) {
			// using the state directly is faulty since the state changes.
		}
	}

	private static class WorkingStateConsumer {

		private final Provider<DynamicState> state;

		@SuppressWarnings ( "unused" )
		WorkingStateConsumer( Provider<DynamicState> state ) {
			this.state = state;
		}

		DynamicState state() {
			return state.provide();
		}
	}

	private static class OuterStateConsumer {

		private final WorkingStateConsumer consumer;

		@SuppressWarnings ( "unused" )
		OuterStateConsumer( WorkingStateConsumer consumer ) {
			this.consumer = consumer;
		}

		WorkingStateConsumer consumer() {
			return consumer;
		}
	}

	private final Injector injector = Bootstrap.injector( ProviderBindsBundle.class );

	@Test
	public void thatProviderIsAvailableForAnyBoundType() {
		assertInjectsProviderFor( "foobar", raw( String.class ) );
		assertInjectsProviderFor( 42, raw( Integer.class ) );
	}

	@Test
	public void thatProviderIsAvailableForAnyNamedBoundType() {
		assertInjectsProviderFor( 846, raw( Integer.class ), named( "foo" ) );
	}

	@Test
	public void thatProviderIsAvailableAlsoForListType() {
		List<String> list = singletonList( "foobar" );
		assertInjectsProviderFor( list, raw( List.class ).parametized( String.class ) );
	}

	@Test
	public void thatProviderIsAvailableAlsoForSetType() {
		Set<String> set = singleton( "foobar" );
		assertInjectsProviderFor( set, raw( Set.class ).parametized( String.class ) );
	}

	@Test
	public void thatProviderMakesPerInjectionInjectableIntoPerApplication() {
		injector.resolve( dependency( WorkingStateConsumer.class ) );
	}

	@Test ( expected = MoreFrequentExpiryException.class )
	public void thatNoProviderCausesExceptionWhenPerInjectionInjectedIntoPerApplication() {
		injector.resolve( dependency( FaultyStateConsumer.class ) );
	}

	@Test
	public void thatProviderKeepsHierarchySoProvidedDependencyIsResolvedAsIfResolvedDirectly() {
		OuterStateConsumer outer = injector.resolve( dependency( OuterStateConsumer.class ) );
		Assert.assertSame( DYNAMIC_STATE_IN_OUTER, outer.consumer().state() );
	}

	@Test
	public void thatProviderCanProvidePerInjectionInstanceWithinAnPerApplicationParent() {
		WorkingStateConsumer obj = injector.resolve( dependency( WorkingStateConsumer.class ) );
		assertNotNull( obj.state() ); // if expiry is a problem this will throw an exception
	}

	private <T> void assertInjectsProviderFor( T expected, Type<? extends T> dependencyType ) {
		assertInjectsProviderFor( expected, dependencyType, Name.ANY );
	}

	private <T> void assertInjectsProviderFor( T expected, Type<? extends T> dependencyType,
			Name name ) {
		Type<? extends Provider<? extends T>> type = providerTypeOf( dependencyType );
		Provider<?> provider = injector.resolve( dependency( type ).named( name ) );
		assertEquals( expected, provider.provide() );
	}
}
