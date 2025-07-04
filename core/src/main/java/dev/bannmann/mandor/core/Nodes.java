package dev.bannmann.mandor.core;

import java.util.Optional;
import java.util.function.Predicate;

import lombok.experimental.UtilityClass;

import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;
import dev.bannmann.labs.core.StreamExtras;

@UtilityClass
public class Nodes
{
    @SuppressWarnings("unchecked")
    @SuppressWarningsRationale("node.findAncestor() is safe, but does not have @SafeVarargs")
    public <T> Optional<T> findAncestor(HasParentNode<Node> node, Class<T> ancestorClass)
    {
        return node.findAncestor(ancestorClass);
    }

    @SuppressWarnings("unchecked")
    @SuppressWarningsRationale("node.findAncestor() is safe, but does not have @SafeVarargs")
    public static <T> Optional<T> findAncestor(HasParentNode<Node> node, Class<T> ancestorClass, Predicate<T> predicate)
    {
        return node.findAncestor(predicate, ancestorClass);
    }

    public String obtainEnclosingTopLevelTypeName(Node startingNode)
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

    public Optional<AnnotationExpr> tryGetSuppressionAnnotation(
        NodeWithAnnotations<?> nodeWithAnnotations,
        String warningName)
    {
        var annotationExprOptional = nodeWithAnnotations.getAnnotationByClass(SuppressWarnings.class);
        if (annotationExprOptional.isEmpty())
        {
            return Optional.empty();
        }

        var annotationExpr = annotationExprOptional.get();
        if (annotationExpr instanceof NormalAnnotationExpr normalAnnotationExpr &&
            normalAnnotationExpr.getPairs()
                .stream()
                .filter(memberValuePair -> memberValuePair.getNameAsString()
                    .equals("value"))
                .map(MemberValuePair::getValue)
                .reduce(StreamExtras.atMostOneThrowing(() -> new UnprocessableSourceCodeException(
                    "Unexpected syntax in @SuppressWarnings annotation")))
                .filter(expression -> suppressesWarning(expression, warningName))
                .isPresent())
        {
            return Optional.of(annotationExpr);
        }

        if (annotationExpr instanceof SingleMemberAnnotationExpr singleMemberAnnotationExpr &&
            suppressesWarning(singleMemberAnnotationExpr.getMemberValue(), warningName))
        {
            return Optional.of(annotationExpr);
        }

        return Optional.empty();
    }

    private static boolean suppressesWarning(Expression expression, String name)
    {
        if (expression instanceof StringLiteralExpr stringLiteralExpr)
        {
            return stringLiteralExpr.getValue()
                .equals(name);
        }
        if (expression instanceof ArrayInitializerExpr arrayInitializerExpr)
        {
            return arrayInitializerExpr.getValues()
                .stream()
                .map(expression1 -> expression1.asStringLiteralExpr()
                    .getValue())
                .anyMatch(value -> value.equals(name));
        }
        throw new UnprocessableSourceCodeException("Unexpected syntax in @SuppressWarnings annotation");
    }

    public static String getQualifiedName(AnnotationExpr annotation, RuleContext context)
    {
        try
        {
            return annotation.resolve()
                .getQualifiedName();
        }
        catch (UnsolvedSymbolException e)
        {
            throw new UnprocessableSourceCodeException(
                "Cannot resolve qualified name for annotation %s used by %s in %s".formatted(annotation.getNameAsString(),
                    obtainEnclosingTopLevelTypeName(annotation),
                    context.getCodeLocation(annotation)),
                e);
        }
    }
}
