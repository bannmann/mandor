package dev.bannmann.mandor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.github.javaparser.ast.CompilationUnit;
import com.google.errorprone.annotations.FormatMethod;

public abstract class SourceRule
{
    private final List<String> violations = new ArrayList<>();

    private @Nullable RuleContext context;

    protected void init(RuleContext context)
    {
        this.context = context;
    }

    protected abstract void scan(CompilationUnit compilationUnit);

    protected final RuleContext getContext()
    {
        if (context == null)
        {
            throw new IllegalStateException();
        }
        return context;
    }

    @FormatMethod
    protected final void addViolation(String message, Object... args)
    {
        violations.add(message.formatted(args));
    }

    public final List<String> getViolations()
    {
        return Collections.unmodifiableList(violations);
    }

    public abstract String getDescription();
}
