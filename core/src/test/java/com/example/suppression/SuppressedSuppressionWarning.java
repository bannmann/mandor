package com.example.suppression;

import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@SuppressWarnings("MissingSuppressionRationale")
@SuppressWarningsRationale("We need to test whether we can suppress MissingSuppressionRationale violations")
public class SuppressedSuppressionWarning
{
    @SuppressWarnings("X:Delta") // No violation of MissingSuppressionRationale due to @SuppressWarnings above
    private static final String GREETING = "Hello, World!";

    @SuppressWarnings("X:Epsilon") // No violation of MissingSuppressionRationale due to @SuppressWarnings above
    public void quux()
    {
    }
}
