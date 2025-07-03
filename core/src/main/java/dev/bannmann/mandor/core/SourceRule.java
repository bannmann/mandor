package dev.bannmann.mandor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.google.errorprone.annotations.FormatMethod;

public abstract class SourceRule
{
    public enum Status
    {
        RECOMMENDED,
        OPTIONAL,
        EXPERIMENTAL
    }

    private final List<String> violations = new ArrayList<>();
    private final MemoStack<Suppression> currentSuppressions = new MemoStack<>();

    private @Nullable RuleContext context;

    protected void init(RuleContext context)
    {
        this.context = context;
    }

    protected abstract void scan(CompilationUnit compilationUnit);

    protected final void trackSuppressibleScope(NodeWithAnnotations<?> nodeWithAnnotations, Runnable action)
    {
        Nodes.tryGetSuppressionAnnotation(nodeWithAnnotations, getClass().getSimpleName())
            .ifPresentOrElse(annotationExpr -> runTracked(annotationExpr, action), action);
    }

    private void runTracked(AnnotationExpr annotationExpr, Runnable action)
    {
        var suppression = new Suppression(annotationExpr);
        try (MemoStack.MemoHandle ignored = currentSuppressions.create(suppression))
        {
            action.run();

            if (!suppression.wasHit())
            {
                violations.add("%s needlessly suppresses %s in %s".formatted(Nodes.obtainEnclosingTopLevelTypeName(
                    annotationExpr), getClass().getSimpleName(), getContext().getCodeLocation(annotationExpr)));
            }
        }
    }

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
        currentSuppressions.accessLastMemoContents()
            .ifPresentOrElse(Suppression::trackHit, () -> violations.add(message.formatted(args)));
    }

    public final List<String> getViolations()
    {
        return Collections.unmodifiableList(violations);
    }

    public abstract String getDescription();

    /**
     * The status of this rule when {@linkplain SourceRuleProvider auto discovery} is used.
     */
    public Status getStatus()
    {
        return Status.OPTIONAL;
    }
}
