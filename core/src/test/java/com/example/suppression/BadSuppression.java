package com.example.suppression;

import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@SuppressWarnings("X:Omega")
@SuppressWarningsRationale(name = "X:Omega", value = "...") // Violation: OvercomplicatedSuppressionRationale
public class BadSuppression
{
    @SuppressWarnings({ "X:Alpha", "X:Beta" }) // Violation: MissingSuppressionRationale
    @SuppressWarningsRationale(name = "X:Alpha", value = "...")
    private static final String FOO = "Stuff";

    @SuppressWarnings("X:Gamma")
    @SuppressWarningsRationale(name = "X:Gamma", value = "...") // Violation: OvercomplicatedSuppressionRationale
    private boolean enabled;

    @SuppressWarnings({ "X:Alpha", "X:Beta" }) // Violation: MissingSuppressionRationale
    @SuppressWarningsRationale("...")
    private int count1;

    @SuppressWarnings({ "X:Beta", "X:Alpha" }) // Violation: MissingSuppressionRationale
    @SuppressWarningsRationale("...")
    private int count2;

    @SuppressWarningsRationale("...") // Violation: OrphanedSuppressionRationale
    public void bar()
    {
    }

    @SuppressWarnings("X:Gamma")
    @SuppressWarningsRationale(value = "...") // Violation: OvercomplicatedSuppressionRationale
    private boolean visible;

    @SuppressWarnings("X:Theta") // Violation: MissingSuppressionRationale
    public void weird()
    {
    }
}
