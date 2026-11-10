package org.springframework.samples.petclinic.system;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This test ensures that a string is translated in every language to avoid partial
 * translations of the API's validation messages.
 *
 * @author Anuj Ashok Potdar
 */
public class I18nPropertiesSyncTest {

	private static final String I18N_DIR = "src/main/resources";

	private static final String BASE_NAME = "messages";

	public static final String PROPERTIES = ".properties";

	@Test
	void checkI18nPropertyFilesAreInSync() throws Exception {
		List<Path> propertyFiles;
		try (Stream<Path> stream = Files.walk(Path.of(I18N_DIR))) {
			propertyFiles = stream.filter(p -> p.getFileName().toString().startsWith(BASE_NAME))
				.filter(p -> p.getFileName().toString().endsWith(PROPERTIES))
				.toList();
		}

		Map<String, Properties> localeToProps = new HashMap<>();

		for (Path path : propertyFiles) {
			Properties props = new Properties();
			try (var reader = Files.newBufferedReader(path)) {
				props.load(reader);
				localeToProps.put(path.getFileName().toString(), props);
			}
		}

		String baseFile = BASE_NAME + PROPERTIES;
		Properties baseProps = localeToProps.get(baseFile);
		if (baseProps == null) {
			fail("Base properties file '" + baseFile + "' not found.");
			return;
		}

		Set<String> baseKeys = baseProps.stringPropertyNames();
		StringBuilder report = new StringBuilder();

		for (Map.Entry<String, Properties> entry : localeToProps.entrySet()) {
			String fileName = entry.getKey();
			// We use fallback logic to include english strings, hence messages_en is not
			// populated.
			if (fileName.equals(baseFile) || "messages_en.properties".equals(fileName)) {
				continue;
			}

			Properties props = entry.getValue();
			Set<String> missingKeys = new TreeSet<>(baseKeys);
			missingKeys.removeAll(props.stringPropertyNames());

			if (!missingKeys.isEmpty()) {
				report.append("Missing keys in ").append(fileName).append(":\n");
				missingKeys.forEach(k -> report.append("  ").append(k).append("\n"));
			}
		}

		if (!report.isEmpty()) {
			fail("Translation files are not in sync:\n" + report);
		}
	}

}
