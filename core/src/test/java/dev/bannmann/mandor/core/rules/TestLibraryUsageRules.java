package dev.bannmann.mandor.core.rules;

import dev.bannmann.mandor.core.AbstractRuleTest;

public class TestLibraryUsageRules extends AbstractRuleTest
{
    public TestLibraryUsageRules()
    {
        super("libraries", new UndesiredThreadSafetyAnnotation());
    }
}
