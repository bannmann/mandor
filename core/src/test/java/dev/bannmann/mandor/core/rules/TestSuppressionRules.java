package dev.bannmann.mandor.core.rules;

import dev.bannmann.mandor.core.AbstractRuleTest;

public class TestSuppressionRules extends AbstractRuleTest
{
    public TestSuppressionRules()
    {
        super("suppression",
            new MissingSuppressionRationale(),
            new OrphanedSuppressionRationale(),
            new OvercomplicatedSuppressionRationale());
    }
}
