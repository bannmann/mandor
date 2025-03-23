package dev.bannmann.mandor.core.rules;

import java.util.Set;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import dev.bannmann.mandor.core.AbstractSourceVisitor;
import dev.bannmann.mandor.core.Context;
import dev.bannmann.mandor.core.SourceRule;

public class UndesiredNullabilityAnnotation extends SourceRule
{
    private static class Visitor extends AbstractSourceVisitor
    {
        private static final Set<String> ANNOTATION_NAMES = Set.of("Nullable", "NotNull", "NonNull", "Nonnull");

        private static final Set<String> ALLOWED_ANNOTATIONS = Set.of("org.jspecify.annotations.NonNull",
            "org.jspecify.annotations.Nullable",
            "lombok.NonNull");

        @Override
        public void visit(MarkerAnnotationExpr markerAnnotation, Context context)
        {
            process(markerAnnotation);
            super.visit(markerAnnotation, context);
        }

        private void process(AnnotationExpr annotation)
        {
            if (!isRelatedToNullness(annotation))
            {
                return;
            }

            String qualifiedName = getQualifiedName(annotation);
            if (ALLOWED_ANNOTATIONS.contains(qualifiedName))
            {
                return;
            }

            if (isOutsideNullMarkedCode(annotation))
            {
                return;
            }

            addViolation("%s uses undesired annotation %s in %s",
                getContext().getEnclosingTypeName(annotation),
                qualifiedName,
                getContext().getFileLocation(annotation));
        }

        private boolean isRelatedToNullness(AnnotationExpr annotation)
        {
            return ANNOTATION_NAMES.contains(annotation.getNameAsString());
        }

        private String getQualifiedName(AnnotationExpr annotation)
        {
            return getContext().resolve(annotation)
                .getQualifiedName();
        }

        private boolean isOutsideNullMarkedCode(AnnotationExpr annotation)
        {
            return !isInsideNullMarkedCode(annotation);
        }

        private boolean isInsideNullMarkedCode(AnnotationExpr annotation)
        {
            return CodeNullness.isInNullMarkedClass(annotation) || CodeNullness.isInNullMarkedPackage(getContext());
        }

        @Override
        public void visit(SingleMemberAnnotationExpr singleMemberAnnotation, Context context)
        {
            process(singleMemberAnnotation);
            super.visit(singleMemberAnnotation, context);
        }

        @Override
        public void visit(NormalAnnotationExpr normalAnnotation, Context context)
        {
            process(normalAnnotation);
            super.visit(normalAnnotation, context);
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
        return "@NullMarked code may only use nullability annotations from jSpecify (and Lombok's @NonNull)";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
