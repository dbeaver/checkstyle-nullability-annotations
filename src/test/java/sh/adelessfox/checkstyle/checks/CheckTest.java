package sh.adelessfox.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.*;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;
import org.jspecify.annotations.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@NullMarked
public class CheckTest {
    private static final Path SOURCES_DIR = Path.of("src/test/resources/source");
    private static final Path CHECKS_DIR = Path.of("src/test/resources/check");
    private static final Path CONFIGS_DIR = Path.of("src/test/resources/config");

    private static @Nullable Locale locale;

    @BeforeAll
    static void beforeAll() {
        locale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        if (locale != null) {
            Locale.setDefault(locale);
        }
    }

    @ParameterizedTest
    @MethodSource("inputs")
    public void fileHasExpectedResults(Input input) throws Exception {
        Configuration configuration = loadConfiguration(input);
        RootModule rootModule = createRootModule(configuration);
        try {
            processAndCheckResults(input, rootModule);
        } finally {
            rootModule.destroy();
        }
    }

    private Configuration loadConfiguration(Input input) throws Exception {
        try (InputStream is = Files.newInputStream(input.config())) {
            return ConfigurationLoader.loadConfiguration(
                new InputSource(is),
                new PropertiesExpander(new Properties()),
                ConfigurationLoader.IgnoredModulesOptions.EXECUTE,
                ThreadModeSettings.SINGLE_THREAD_MODE_INSTANCE
            );
        }
    }

    private RootModule createRootModule(Configuration configuration) throws CheckstyleException {
        ClassLoader classLoader = getClass().getClassLoader();
        ModuleFactory factory = new PackageObjectFactory(Checker.class.getPackage().getName(), classLoader);
        RootModule rootModule = (RootModule) factory.createModule(configuration.getName());
        rootModule.setModuleClassLoader(classLoader);
        rootModule.configure(configuration);
        return rootModule;
    }

    private void processAndCheckResults(Input input, RootModule rootModule) throws CheckstyleException {
        rootModule.addListener(input.assertionsAuditListener());
        rootModule.process(List.of(input.source().toFile()));
    }

    private static List<Input> inputs() throws IOException {
        List<Input> inputs = new ArrayList<>();
        try (Stream<Path> stream = Files.list(SOURCES_DIR)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                inputs.add(input(path));
            }
        }
        return inputs.stream()
            .sorted(Comparator.comparing(Input::source))
            .toList();
    }

    private static Input input(Path sourcePath) throws IOException {
        var name = SOURCES_DIR.relativize(sourcePath).toString().replace(".java", "");
        var configPath = CONFIGS_DIR.resolve(name + ".xml");
        if (Files.notExists(configPath)) {
            configPath = CONFIGS_DIR.resolve("default.xml");
        }
        var checkPath = CHECKS_DIR.resolve(name + ".txt");
        return new Input(sourcePath, configPath, new AssertionsAuditListener(Files.readAllLines(checkPath)));
    }

    public record Input(Path source, Path config, AssertionsAuditListener assertionsAuditListener) {
        @Override
        public String toString() {
            return source.toString();
        }
    }
}
