package project;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record Banner(String name) implements ToolProvider {
	public Banner() {
		this("banner");
	}

	@Override
	public int run(PrintWriter out, PrintWriter err, String... args) {
		return 0;
	}
}
