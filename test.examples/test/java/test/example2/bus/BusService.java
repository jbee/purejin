package test.example2.bus;

import test.example2.Bus;
import test.example2.Car;

public class BusService {

	public Bus toLondon() {
		return new Bus("London");
	}

	public Bus toGlasgow() {
		return new Bus("Glasgow");
	}

	/**
	 * This should not be picked as it does not return a {@link Bus}
	 */
	public Car carOfBusDriver() {
		return new Car("bus-driver-to-London");
	}
}
