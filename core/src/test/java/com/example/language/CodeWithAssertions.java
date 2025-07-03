package com.example.language;

public class CodeWithAssertions
{
    private boolean initialized;

    public void foo(int x, int y)
    {
        assert x != y; // Violation: AssertStatementUsage

        assert x > 0 : "x should be positive"; // Violation: AssertStatementUsage
    }

    @SuppressWarnings("AssertStatementUsage")
    public void bar(int x, int y)
    {
        assert initialized; // No violation of AssertStatementUsage due to @SuppressWarnings above

        // stuff happens here
    }

    @SuppressWarnings("AssertStatementUsage") // Violation: needless suppression
    public void quux(int x, int y)
    {
        // stuff happens here, too
    }
}
