package dev.bannmann.mandor.core.rules;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;
import dev.bannmann.mandor.core.UnprocessableSourceCodeException;

@MetaInfServices
public class RedundantlyNullMarkedCode extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        @Override
        public void visit(MarkerAnnotationExpr markerAnnotation, Void arg)
        {
            process(markerAnnotation);
            super.visit(markerAnnotation, arg);
        }

        private void process(AnnotationExpr annotation)
        {
            if (annotation.getParentNode()
                .filter(PackageDeclaration.class::isInstance)
                .isPresent())
            {
                // "Package" is the top level for our checks, so it makes no sense to hunt for redundancies.
                return;
            }

            if (annotationHasMismatchingSimpleName(annotation))
            {
                return;
            }

            var resolvedAnnotationDeclaration = annotation.resolve();
            if (annotationHasMismatchingType(resolvedAnnotationDeclaration))
            {
                return;
            }

            // Before searching for higher-level @NullMarked, we skip the node where we found the current @NullMarked
            Node startingNode = annotation.getParentNode()
                .flatMap(Node::getParentNode)
                .orElseThrow(() -> new CodeInconsistencyException("Annotation " + annotation + " has no parent node"));

            var foundAncestorOptional = findAncestorWithAnnotation(startingNode);
            if (foundAncestorOptional.isPresent())
            {
                NodeWithAnnotations<?> foundAncestor = foundAncestorOptional.get();
                if (isNullUnmarked(foundAncestor))
                {
                    // The original @NullMarked at our starting node overrides an ancestor's @NullUnmarked. Good!
                    return;
                }

                addViolation("Despite the enclosing scope already being @NullMarked, %s specifies it again in %s",
                    Nodes.obtainEnclosingTopLevelTypeName(annotation),
                    getContext().getCodeLocation(annotation));
                return;
            }

            /*
             * If we get here, the enclosing scopes like methods and classes are neither NullMarked nor NullUnmarked.
             * So we need to analyze the package-info, keeping in mind that there may be multiple of those (e.g.
             * src/main vs src/test).
             */
            Set<AnnotationExpr> nullabilityAnnotations = getContext().getPackageInfoFiles()
                .flatMap(packageDeclaration -> packageDeclaration.getAnnotations()
                    .stream())
                .filter(annotationExpr -> annotationExpr.getNameAsString()
                                              .equals("NullMarked") ||
                                          annotationExpr.getNameAsString()
                                              .equals("NullUnmarked"))
                .collect(Collectors.toSet());
            if (nullabilityAnnotations.isEmpty())
            {
                // The original @NullMarked at our starting node is the topmost statement we could find. Good!
                return;
            }

            if (nullabilityAnnotations.size() > 1)
            {
                throw new UnprocessableSourceCodeException(
                    "Conflicting NullMarked/NullUnmarked annotations on package of " + getContext().getFilePath());
            }

            /*
             * If we get here, there is only NullMarked or NullUnmarked in the package-info (or in all package-infos).
             * Let's check whether the original @NullMarked at our starting node is redundant.
             */

            if (nullabilityAnnotations.iterator()
                .next()
                .getNameAsString()
                .equals("NullUnmarked"))
            {
                // The original @NullMarked at our starting node overrides the package's @NullUnmarked.
                return;
            }

            addViolation("Despite the package already being @NullMarked, %s specifies it again in %s",
                Nodes.obtainEnclosingTopLevelTypeName(annotation),
                getContext().getCodeLocation(annotation));
        }

        private boolean annotationHasMismatchingSimpleName(AnnotationExpr annotation)
        {
            return !annotation.getNameAsString()
                .equals(NullMarked.class.getSimpleName());
        }

        private boolean annotationHasMismatchingType(ResolvedAnnotationDeclaration resolvedAnnotationDeclaration)
        {
            return !resolvedAnnotationDeclaration.getQualifiedName()
                .equals(NullMarked.class.getName());
        }

        private Optional<NodeWithAnnotations<?>> findAncestorWithAnnotation(Node startingNode)
        {
            Node currentNode = startingNode;
            while (currentNode != null)
            {
                if (currentNode instanceof NodeWithAnnotations<?> nodeWithAnnotations &&
                    isNullMarkedOrUnmarked(nodeWithAnnotations))
                {
                    return Optional.of(nodeWithAnnotations);
                }

                currentNode = currentNode.getParentNode()
                    .orElse(null);
            }
            return Optional.empty();
        }

        private boolean isNullMarkedOrUnmarked(NodeWithAnnotations<?> nodeWithAnnotations)
        {
            return isNullMarked(nodeWithAnnotations) || isNullUnmarked(nodeWithAnnotations);
        }

        private boolean isNullMarked(NodeWithAnnotations<?> nodeWithAnnotations)
        {
            return nodeWithAnnotations.isAnnotationPresent(NullMarked.class);
        }

        private boolean isNullUnmarked(NodeWithAnnotations<?> nodeWithAnnotations)
        {
            return nodeWithAnnotations.isAnnotationPresent(NullUnmarked.class);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
        }

        @Override
        public void visit(FieldDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
        }

        @Override
        public void visit(MethodDeclaration n, Void arg)
        {
            trackSuppressibleScope(n, () -> super.visit(n, arg));
        }
    }

    private final Visitor visitor = new Visitor();

    @Override
    protected void scan(CompilationUnit compilationUnit)
    {
        compilationUnit.accept(visitor, null);
    }

    @Override
    public String getDescription()
    {
        return "@NullMarked should not be used inside code that is already @NullMarked";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }

    @Override
    public Status getStatus()
    {
        return Status.RECOMMENDED;
    }
}
