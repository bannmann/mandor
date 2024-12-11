package dev.bannmann.mandor.core;

import static dev.bannmann.labs.core.Nullness.guaranteeNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.mizool.core.exception.CodeInconsistencyException;
import com.google.errorprone.annotations.FormatMethod;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

public abstract class AbstractSourceVisitor extends VoidVisitorAdapter<Void>
{
    private final List<String> violations = new ArrayList<>();

    private @Nullable CompilationUnit compilationUnit;

    @Override
    public void visit(CompilationUnit compilationUnit, Void arg)
    {
        this.compilationUnit = compilationUnit;
        super.visit(compilationUnit, arg);
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

    protected Path getPath()
    {
        return guaranteeNonNull(compilationUnit).getStorage()
            .map(CompilationUnit.Storage::getPath)
            .orElseThrow(IllegalStateException::new);
    }

    protected String getEnclosingTypeName(Node startingNode)
    {
        String topmostTypeName = null;
        Node node = startingNode;
        while (node != null)
        {
            if (node instanceof TypeDeclaration<?> typeDeclaration)
            {
                topmostTypeName = typeDeclaration.getFullyQualifiedName()
                    .orElse(topmostTypeName);
            }
            node = node.getParentNode()
                .orElse(null);
        }

        if (topmostTypeName == null)
        {
            throw new IllegalArgumentException("%s doesn't seem to have any enclosing type".formatted(getPath()));
        }

        return topmostTypeName;
    }

    protected String getFileLocation(Node startingNode)
    {
        return "(%s:%s)".formatted(getPath().getFileName(),
            startingNode.getRange()
                .map(range -> range.begin.line)
                .orElseThrow(CodeInconsistencyException::new));
    }

    @SuppressWarnings("unchecked")
    @SuppressWarningsRationale("findAncestor() is safe, but does not have @SafeVarargs")
    protected <T> Optional<T> findAncestor(HasParentNode<Node> node, Class<T> ancestorClass)
    {
        return node.findAncestor(ancestorClass);
    }

    protected boolean nodesAreDifferent(Node a, Node b)
    {
        return !nodesAreTheSame(a, b);
    }

    @SuppressWarnings({ "ReferenceEquality", "BooleanMethodIsAlwaysInverted" })
    @SuppressWarningsRationale(name = "ReferenceEquality",
        value = "This method is intended for navigation in the syntax tree in case reference equality is needed")
    @SuppressWarningsRationale(name = "BooleanMethodIsAlwaysInverted", value = "We want to offer both methods.")
    protected boolean nodesAreTheSame(Node a, Node b)
    {
        return a == b;
    }
}
