package test.example2.car.pool;

import test.example2.Bus;
import test.example2.Car;

public class CarPoolService {

	public Car[] poolCars() {
		return new Car[] { new Car("pool1"), new Car("pool2") };
	}

	public Car nextPoolCar() {
		return null;
	}

	public Bus stopAtCarPool() {
		return new Bus("stop-at-car-pool");
	}
}
