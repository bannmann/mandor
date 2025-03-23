package dev.bannmann.mandor.core;

import static dev.bannmann.labs.core.Nullness.guaranteeNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.errorprone.annotations.FormatMethod;

public abstract class AbstractSourceVisitor extends VoidVisitorAdapter<Context>
{
    private final List<String> violations = new ArrayList<>();

    private @Nullable Context context;

    @Override
    public void visit(CompilationUnit compilationUnit, Context context)
    {
        this.context = context;
        super.visit(compilationUnit, context);
    }

    @FormatMethod
    protected void addViolation(String message, Object... args)
    {
        violations.add(message.formatted(args));
    }

    public List<String> getViolations()
    {
        return Collections.unmodifiableList(violations);
    }

    protected Context getContext()
    {
        return guaranteeNonNull(context);
    }
}
