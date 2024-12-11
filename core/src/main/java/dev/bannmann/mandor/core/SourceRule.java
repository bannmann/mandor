package dev.bannmann.mandor.core;

import java.util.List;

import com.github.javaparser.ast.CompilationUnit;

public abstract class SourceRule
{
    final void scan(CompilationUnit compilationUnit)
    {
        AbstractSourceVisitor visitor = getVisitor();
        compilationUnit.accept(visitor, null);
    }

    protected abstract AbstractSourceVisitor getVisitor();

    public abstract String getDescription();

    public List<String> getViolations()
    {
        return getVisitor().getViolations();
    }
}
