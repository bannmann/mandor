package dev.bannmann.mandor.core.rules;

import java.util.Optional;

import lombok.experimental.UtilityClass;

import org.jspecify.annotations.NullMarked;

import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.RuleContext;

@UtilityClass
class CodeNullness
{
    public static boolean isInNullMarkedClass(HasParentNode<Node> node)
    {
        return Nodes.findAncestor(node, CodeNullness::hasNullMarkedAnnotation, TypeDeclaration.class)
            .isPresent();
    }

    private static boolean hasNullMarkedAnnotation(TypeDeclaration<?> o)
    {
        return CodeNullness.getNullMarkedAnnotation(o)
            .isPresent();
    }

    private static Optional<AnnotationExpr> getNullMarkedAnnotation(NodeWithAnnotations<?> node)
    {
        return node.getAnnotationByClass(NullMarked.class);
    }

    public static boolean isInNullMarkedPackage(RuleContext context)
    {
        return context.getPackageInfoFiles()
            .flatMap(packageDeclaration -> getNullMarkedAnnotation(packageDeclaration).stream())
            .findAny()
            .isPresent();
    }
}
