package techguns.client.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import techguns.Techguns;

/** Results of a client test run: PASS / FAIL / INFO lines by section, written to report.txt at the end. */
public class ClientTestReport {
	private final List<String> lines = new ArrayList<>();
	private int passes, fails, infos;

	public void pass(String section, String msg) {
		passes++;
		add("PASS", section, msg);
	}

	public void fail(String section, String msg) {
		fails++;
		add("FAIL", section, msg);
		Techguns.LOGGER.error("[TG-CLIENTTEST] FAIL {}: {}", section, msg);
	}

	public void info(String section, String msg) {
		infos++;
		add("INFO", section, msg);
	}

	private void add(String kind, String section, String msg) {
		lines.add(String.format("%-4s %-12s %s", kind, section, msg));
	}

	public int fails() {
		return fails;
	}

	public String summary() {
		return passes + " passed, " + fails + " failed, " + infos + " info";
	}

	public void write(Path file) throws IOException {
		List<String> out = new ArrayList<>();
		out.add("Techguns client test: " + summary());
		out.add("");
		lines.stream().filter(l -> l.startsWith("FAIL")).forEach(out::add);
		out.add("");
		out.add("---- all results, in order ----");
		out.addAll(lines);
		Files.createDirectories(file.getParent());
		Files.write(file, out, StandardCharsets.UTF_8);
	}
}
