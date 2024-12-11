package dev.bannmann.mandor.core.rules;

import java.util.Optional;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.labs.core.StreamExtras;
import dev.bannmann.mandor.core.AbstractSourceVisitor;
import dev.bannmann.mandor.core.SourceRule;

public class OvercomplicatedSuppressionRationale extends SourceRule
{
    private static class Visitor extends AbstractSourceVisitor
    {
        @Override
        public void visit(NormalAnnotationExpr annotation, Void unused)
        {
            super.visit(annotation, unused);
            processAnnotation(annotation);
        }

        private void processAnnotation(NormalAnnotationExpr annotation)
        {
            if (isSuppressWarningsRationale(annotation))
            {
                validateMultipleSuppressionsExist(annotation);
            }
        }

        private boolean isSuppressWarningsRationale(AnnotationExpr annotation)
        {
            return annotation.getNameAsString()
                .equals("SuppressWarningsRationale") ||
                annotation.getNameAsString()
                    .equals("dev.bannmann.labs.annotations.SuppressWarningsRationale");
        }

        private void validateMultipleSuppressionsExist(NormalAnnotationExpr annotation)
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

            if (suppressionAnnotationOptional.map(SingleMemberAnnotationExpr::getMemberValue)
                .filter(Expression::isArrayInitializerExpr)
                .isEmpty())
            {
                addViolation("%s needlessly specifies a suppression name for a rationale in %s",
                    getEnclosingTypeName(annotation),
                    getFileLocation(annotation));
            }
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
        return "@SuppressWarningsRationale may only specify a 'name' if there is more than one suppression";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
