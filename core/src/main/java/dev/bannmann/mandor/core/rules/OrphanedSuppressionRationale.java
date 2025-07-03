package dev.bannmann.mandor.core.rules;

import java.util.Optional;

import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.labs.core.StreamExtras;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;

@MetaInfServices
public final class OrphanedSuppressionRationale extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        @Override
        public void visit(SingleMemberAnnotationExpr annotation, Void unused)
        {
            processAnnotation(annotation);
            super.visit(annotation, unused);
        }

        private void processAnnotation(AnnotationExpr annotation)
        {
            if (isSuppressWarningsRationale(annotation))
            {
                validateSuppressionExists(annotation);
            }
        }

        private boolean isSuppressWarningsRationale(AnnotationExpr annotation)
        {
            return annotation.getNameAsString()
                       .equals("SuppressWarningsRationale") ||
                   annotation.getNameAsString()
                       .equals("dev.bannmann.labs.annotations.SuppressWarningsRationale");
        }

        private void validateSuppressionExists(AnnotationExpr annotation)
        {
            Optional<SingleMemberAnnotationExpr> suppressionAnnotationOptional = annotation.getParentNode()
                .orElseThrow(CodeInconsistencyException::new)
                .getChildNodes()
                .stream()
                .filter(SingleMemberAnnotationExpr.class::isInstance)
                .map(SingleMemberAnnotationExpr.class::cast)
                .filter(annotationExpr -> annotationExpr.getNameAsString()
                    .equals("SuppressWarnings"))
                .reduce(StreamExtras.atMostOneThrowing(() -> new CodeInconsistencyException(
                    "SuppressWarnings is not supposed to be repeatable")));

            if (suppressionAnnotationOptional.isEmpty())
            {
                addViolation("%s gives a rationale without suppressing a warning in %s",
                    Nodes.getEnclosingTypeName(annotation),
                    getContext().getCodeLocation(annotation));
            }
        }

        @Override
        public void visit(NormalAnnotationExpr annotation, Void arg)
        {
            processAnnotation(annotation);
            super.visit(annotation, arg);
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
        return "Each @SuppressWarningsRationale must have a corresponding @SuppressWarnings";
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
