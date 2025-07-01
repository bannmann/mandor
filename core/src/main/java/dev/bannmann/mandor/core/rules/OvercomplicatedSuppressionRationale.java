package dev.bannmann.mandor.core.rules;

import java.util.Optional;

import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.labs.core.StreamExtras;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;

@MetaInfServices
public final class OvercomplicatedSuppressionRationale extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
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
                if (specifiesName(annotation))
                {
                    validateMultipleSuppressionsExist(annotation);
                }

                if (shouldUseSingleMemberForm(annotation))
                {
                    addViolation("%s needlessly uses the full `value=\"…\"` syntax for a rationale in %s",
                        Nodes.getEnclosingTypeName(annotation),
                        getContext().getCodeLocation(annotation));
                }
            }
        }

        private boolean isSuppressWarningsRationale(AnnotationExpr annotation)
        {
            return annotation.getNameAsString()
                       .equals("SuppressWarningsRationale") ||
                   annotation.getNameAsString()
                       .equals("dev.bannmann.labs.annotations.SuppressWarningsRationale");
        }

        private boolean specifiesName(AnnotationExpr annotation)
        {
            return annotation instanceof NormalAnnotationExpr normalAnnotationExpr &&
                   normalAnnotationExpr.getPairs()
                       .stream()
                       .map(NodeWithSimpleName::getNameAsString)
                       .anyMatch(name -> name.equals("name"));
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
                    Nodes.getEnclosingTypeName(annotation),
                    getContext().getCodeLocation(annotation));
            }
        }

        private boolean shouldUseSingleMemberForm(NormalAnnotationExpr annotation)
        {
            return annotation.getPairs()
                .stream()
                .map(MemberValuePair::getNameAsString)
                .allMatch(name -> name.equals("value"));
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg)
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
        return "@SuppressWarningsRationale may only specify a 'name' if there is more than one suppression";
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
