package se.jbee.inject.config;

import static se.jbee.inject.util.TestUtils.assertSerializable;

import javax.swing.text.html.FormSubmitEvent.MethodType;

import org.junit.Test;

public class TestConfigSerializable {

	@Test
	public void choicesIsSerializable() {
		assertSerializable(Choices.NONE);
		assertSerializable(Choices.NONE.choose(MethodType.GET));
	}
}
