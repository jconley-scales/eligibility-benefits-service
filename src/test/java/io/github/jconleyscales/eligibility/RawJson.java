package io.github.jconleyscales.eligibility;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads values out of a JSON body exactly as the service spelled them. JSON comparison treats 30.00 and 30 as the
 * same number, so the tests of the units the API promises read the raw text instead.
 */
final class RawJson {

	private RawJson() {
	}

	/**
	 * Every value of a JSON member with this name, exactly as the response spells it.
	 */
	static List<String> valuesOf(String json, String name) {
		return Pattern.compile("\"" + name + "\"\\s*:\\s*([^,}\\s]+)")
			.matcher(json)
			.results()
			.map(match -> match.group(1))
			.toList();
	}

}
