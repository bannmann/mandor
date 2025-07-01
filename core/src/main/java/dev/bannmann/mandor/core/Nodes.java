package dev.bannmann.mandor.core;

import java.util.Optional;
import java.util.function.Predicate;

import lombok.experimental.UtilityClass;

import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@UtilityClass
public class Nodes
{
    @SuppressWarnings("unchecked")
    @SuppressWarningsRationale("findAncestor() is safe, but does not have @SafeVarargs")
    public <T> Optional<T> findAncestor(HasParentNode<Node> node, Class<T> ancestorClass)
    {
        return node.findAncestor(ancestorClass);
    }

    @SuppressWarnings("unchecked")
    @SuppressWarningsRationale("findAncestor() is safe, but does not have @SafeVarargs")
    public static <T> Optional<T> findAncestor(HasParentNode<Node> node, Predicate<T> predicate, Class<T> ancestorClass)
    {
        return node.findAncestor(predicate, ancestorClass);
    }

    public String getEnclosingTypeName(Node startingNode)
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
            throw new UnprocessableSourceCodeException("Node doesn't seem to have any enclosing type");
        }

        return topmostTypeName;
    }

    public boolean areDifferent(Node a, Node b)
    {
        return !areTheSame(a, b);
    }

    @SuppressWarnings({ "ReferenceEquality", "BooleanMethodIsAlwaysInverted" })
    @SuppressWarningsRationale(name = "ReferenceEquality",
        value = "This method is intended for navigation in the syntax tree in case reference equality is needed")
    @SuppressWarningsRationale(name = "BooleanMethodIsAlwaysInverted", value = "We want to offer both methods.")
    public boolean areTheSame(Node a, Node b)
    {
        return a == b;
    }
}
