package dev.bannmann.mandor.core.rules;

import dev.bannmann.mandor.core.AbstractRuleTest;

public class TestNullnessRules extends AbstractRuleTest
{
    public TestNullnessRules()
    {
        super("nullness",
            new NullabilityAnnotationOutsideNullMarkedCode(),
            new RedundantlyNullMarkedCode(),
            new UndesiredNullabilityAnnotation());
    }
}
