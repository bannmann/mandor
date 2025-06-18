package dev.bannmann.mandor.core.rules;

import java.util.Optional;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.labs.core.StreamExtras;
import dev.bannmann.mandor.core.AbstractSourceVisitor;
import dev.bannmann.mandor.core.Context;
import dev.bannmann.mandor.core.SourceRule;

public class OrphanedSuppressionRationale extends SourceRule
{
    private static class Visitor extends AbstractSourceVisitor
    {
        @Override
        public void visit(SingleMemberAnnotationExpr annotation, Context context)
        {
            processAnnotation(annotation);
            super.visit(annotation, context);
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
                    getContext().getEnclosingTypeName(annotation),
                    getContext().getFileLocation(annotation));
            }
        }

        @Override
        public void visit(NormalAnnotationExpr annotation, Context context)
        {
            processAnnotation(annotation);
            super.visit(annotation, context);
        }
    }

    private final Visitor visitor = new Visitor();

    @Override
    protected AbstractSourceVisitor getVisitor()
    {
        return visitor;
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
}
