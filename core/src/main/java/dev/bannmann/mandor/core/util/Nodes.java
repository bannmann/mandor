package dev.bannmann.mandor.core.util;

import java.util.Optional;

import lombok.experimental.UtilityClass;

import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.Node;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@UtilityClass
public class Nodes
{
    @SuppressWarnings("unchecked")
    @SuppressWarningsRationale("findAncestor() is safe, but does not have @SafeVarargs")
    public static <T> Optional<T> findAncestor(HasParentNode<Node> node, Class<T> ancestorClass)
    {
        return node.findAncestor(ancestorClass);
    }

    public static boolean nodesAreDifferent(Node a, Node b)
    {
        return !nodesAreTheSame(a, b);
    }

    @SuppressWarnings({ "ReferenceEquality", "BooleanMethodIsAlwaysInverted" })
    @SuppressWarningsRationale(name = "ReferenceEquality",
        value = "This method is intended for navigation in the syntax tree in case reference equality is needed")
    @SuppressWarningsRationale(name = "BooleanMethodIsAlwaysInverted", value = "We want to offer both methods.")
    public static boolean nodesAreTheSame(Node a, Node b)
    {
        return a == b;
    }
}
