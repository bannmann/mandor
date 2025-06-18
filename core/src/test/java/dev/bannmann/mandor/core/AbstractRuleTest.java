package dev.bannmann.mandor.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.io.Resources;
import net.greypanther.natsort.SimpleNaturalComparator;

public abstract class AbstractRuleTest
{
    public static final Path ROOT_PATH = Paths.get("src/test/java")
        .toAbsolutePath();
    private final List<SourceRule> rules;
    private final SourceBundle sourceBundle;
    private Path ruleResultsDirectory;

    protected AbstractRuleTest(String packageNameSegment, SourceRule... rules)
    {
        this.rules = ImmutableList.copyOf(rules);

        sourceBundle = new SourceBundle().importSources(ROOT_PATH, pathContains(packageNameSegment));

        ruleResultsDirectory = Paths.get("target", "rule-results");
        try
        {
            Files.createDirectories(ruleResultsDirectory);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static Predicate<Path> pathContains(String segment)
    {
        return path -> Iterators.contains(ROOT_PATH.relativize(path.toAbsolutePath())
            .iterator(), Path.of(segment));
    }

    @DataProvider
    protected Object[][] rules()
    {
        var result = new Object[rules.size()][1];
        for (int i = 0; i < rules.size(); i++)
        {
            result[i][0] = rules.get(i);
        }
        return result;
    }

    @Test(dataProvider = "rules")
    public void test(SourceRule rule) throws IOException
    {
        String resultsFileName = "%s-%s.txt".formatted(getClass().getSimpleName(),
            rule.getClass()
                .getSimpleName());

        List<String> expected = getExpectation(resultsFileName);

        List<String> actual = sourceBundle.runScan(rule)
            .stream()
            .sorted(SimpleNaturalComparator.getInstance())
            .toList();

        Files.write(ruleResultsDirectory.resolve(resultsFileName), actual, StandardCharsets.UTF_8);

        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private @NonNull List<String> getExpectation(String resultsFileName) throws IOException
    {
        URL url;
        try
        {
            url = Resources.getResource(getClass(), resultsFileName);
        }
        catch (IllegalArgumentException e)
        {
            return List.of();
        }

        return Resources.readLines(url, StandardCharsets.UTF_8);
    }
}
