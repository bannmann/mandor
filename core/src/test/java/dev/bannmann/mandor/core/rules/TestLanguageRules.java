package dev.bannmann.mandor.core.rules;

import dev.bannmann.mandor.core.AbstractRuleTest;

public class TestLanguageRules extends AbstractRuleTest
{
    public TestLanguageRules()
    {
        super("language", new AssertStatementUsage(), new BitwiseOperatorUsage(), new OctalNumberUsage());
    }
}
