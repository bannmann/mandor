package dev.bannmann.mandor;

import static dev.bannmann.mandor.core.SourceRule.Status.EXPERIMENTAL;
import static dev.bannmann.mandor.core.SourceRule.Status.OPTIONAL;
import static dev.bannmann.mandor.core.SourceRule.Status.RECOMMENDED;

import java.nio.file.Path;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import dev.bannmann.mandor.core.SourceBundle;
import dev.bannmann.mandor.core.SourceRule;
import dev.bannmann.mandor.core.SourceRuleProvider;

public class TestRulesOnMandorCodebase
{
    private final SourceBundle sourceBundle = new SourceBundle().importSources("src/main/java")
        .importSources(Path.of("src/test/java"),
            path -> !path.toString()
                .contains("example"));

    @DataProvider
    public static Object[][] rules()
    {
        return SourceRuleProvider.load()
            .customFrom("dev.bannmann.mandor.core.rules", RECOMMENDED, OPTIONAL, EXPERIMENTAL)
            .asDataProvider();
    }

    @Test(dataProvider = "rules")
    public void testRule(SourceRule sourceRule)
    {
        sourceBundle.verify(sourceRule);
    }
}
