package dev.bannmann.mandor.core.rules;

import java.util.Optional;

import lombok.experimental.UtilityClass;

import org.jspecify.annotations.NullMarked;

import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import dev.bannmann.mandor.core.Context;
import dev.bannmann.mandor.core.util.Nodes;

@UtilityClass
class CodeNullness
{
    public static boolean isInNullMarkedClass(HasParentNode<Node> node)
    {
        return Nodes.findAncestor(node, TypeDeclaration.class)
            .flatMap(CodeNullness::getNullMarkedAnnotation)
            .isPresent();
    }

    private static Optional<AnnotationExpr> getNullMarkedAnnotation(NodeWithAnnotations<?> node)
    {
        return node.getAnnotationByClass(NullMarked.class);
    }

    public static boolean isInNullMarkedPackage(Context context)
    {
        return context.getPackageInfoFiles()
            .flatMap(packageDeclaration -> getNullMarkedAnnotation(packageDeclaration).stream())
            .findAny()
            .isPresent();
    }
}
