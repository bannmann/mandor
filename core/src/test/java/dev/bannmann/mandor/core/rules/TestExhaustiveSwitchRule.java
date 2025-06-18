package dev.bannmann.mandor.core.rules;

import dev.bannmann.mandor.core.AbstractRuleTest;

public class TestExhaustiveSwitchRule extends AbstractRuleTest
{
    public TestExhaustiveSwitchRule()
    {
        super("exhaustive_switch", new MalformedExhaustiveSwitch());
    }
}
