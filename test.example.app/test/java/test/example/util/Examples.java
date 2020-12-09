package test.example.util;

public @interface Examples {

	enum Example {
		Example1
	}

	//TODO use edition/features to filter the actually bootstrapped bundles via ServiceLoader

	Example value();
}
