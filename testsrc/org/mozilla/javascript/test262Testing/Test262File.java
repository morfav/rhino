package org.mozilla.javascript.test262Testing;

import org.mozilla.javascript.tools.SourceReader;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@SuppressWarnings("unchecked")
public class Test262File {

    private static final Yaml YAML = new Yaml();
    private static final String testHarnessDir = "test262/harness/";

    // Use custom sta.js as the original contains destructuring syntax not supported by rhino
    public static final String CUSTOM_STA_FILE = "testsrc/org/mozilla/javascript/test262Testing/harness/sta.js";

    private final String testSource;
    private final Set<String> harnessFiles = new HashSet<>();
    private final Map<String, Object> metadata;
    private String expectedError = null;
    private boolean isEarly;
    private final Set<String> flags = new HashSet<>();
    private final Set<String> features = new HashSet<>();

    public Test262File(File testFile) throws IOException {
        String filePath = testFile.getPath();

        testSource = readTestSource(filePath);

        metadata = readMetaData();

        harnessFiles.addAll(readHarnessFiles(filePath));

        if (metadata.containsKey("negative")) {
            Map<String, String> negative = (Map<String, String>) metadata.get("negative");
            expectedError = negative.get("type");
            isEarly = "early".equals(negative.get("phase"));
        }

        flags.addAll(readFlags());

        features.addAll(readFeatures());
    }

    private String readTestSource(String filePath) throws IOException {
        return (String) SourceReader.readFileOrUrl(filePath, true, "UTF-8");
    }

    private Map<String, Object> readMetaData() {
        String metadataStr = testSource.substring(
                testSource.indexOf("/*---") + 5,
                testSource.indexOf("---*/"));
        return YAML.load(metadataStr);
    }

    private Set<String> readHarnessFiles(String filePath) {
        Set<String> harnessFiles = new HashSet<>();
        if (metadata.containsKey("includes")) {
            harnessFiles.addAll(
                    ((List<String>) metadata.get("includes")).stream()
                            .map(fileName -> testHarnessDir + fileName)
                            .collect(Collectors.toSet())
            );
        }

        if (!flags.contains("raw")) {
            // present by default harness files
            harnessFiles.add(testHarnessDir + "assert.js");
            // Use custom sta.js as the original contains destructuring syntax not supported by rhino
            harnessFiles.add(CUSTOM_STA_FILE);
        } else if (!harnessFiles.isEmpty()) {
            System.err.format(
                    "WARN: case '%s' is flagged as 'raw' but also has defined includes%n",
                    filePath);
        }
        return harnessFiles;
    }

    private Collection<String> readFlags() {
        if (!metadata.containsKey("flags")) {
            return emptySet();
        }
        return (Collection<String>) metadata.get("flags");
    }

    private Collection<String> readFeatures() {
        if (!metadata.containsKey("features")) {
            return emptySet();
        }
        return (Collection<String>) metadata.get("features");
    }

    public String getTestSource() {
        return testSource;
    }

    public Set<String> getHarnessFiles() {
        return harnessFiles;
    }

    public String getExpectedError() {
        return expectedError;
    }

    public boolean isEarly() {
        return isEarly;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public Set<String> getFeatures() {
        return features;
    }
}
