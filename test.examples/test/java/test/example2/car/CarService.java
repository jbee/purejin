package test.example2.car;

import test.example2.Bus;
import test.example2.Car;

public class CarService {

	public Car getPetersCar() {
		return new Car("Peter");
	}

	public Car getPaulsCar() {
		return new Car("Paul");
	}

	public Bus busStopAtCarPark() {
		return new Bus("at-car-park");
	}

}
