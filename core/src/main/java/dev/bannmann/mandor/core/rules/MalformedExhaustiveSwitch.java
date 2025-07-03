package dev.bannmann.mandor.core.rules;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;

@MetaInfServices
public final class MalformedExhaustiveSwitch extends SourceRule
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
            if (!annotation.getNameAsString()
                .equals("ExhaustiveSwitch"))
            {
                return;
            }

            if (!isUsedCorrectly(annotation))
            {
                addViolation("%s uses @ExhaustiveSwitch incorrectly in %s",
                    Nodes.obtainEnclosingTopLevelTypeName(annotation),
                    getContext().getCodeLocation(annotation));
            }
        }

        private boolean isUsedCorrectly(AnnotationExpr annotation)
        {
            Node assignmentNode = annotation.getParentNode()
                .orElseThrow(CodeInconsistencyException::new);

            List<Node> nodes = assignmentNode.getChildNodes();
            if (nodes.size() != 2 || Nodes.areDifferent(nodes.get(0), annotation) ||
                !(nodes.get(1) instanceof VariableDeclarator variableDeclarator) ||
                variableDeclarator.getInitializer()
                    .filter(Expression::isSwitchExpr)
                    .isEmpty())
            {
                return false;
            }

            SimpleName assignedVariableName = variableDeclarator.getName();

            ExpressionStmt assignmentStatement = Nodes.findAncestor(assignmentNode, ExpressionStmt.class)
                .orElseThrow(CodeInconsistencyException::new);

            return getNextSiblingNode(assignmentStatement) instanceof ExpressionStmt nextStatement &&
                   isMethodCallViaVariable(nextStatement, assignedVariableName);
        }

        private @Nullable Node getNextSiblingNode(Node startingNode)
        {
            return startingNode.getParentNode()
                .orElseThrow(CodeInconsistencyException::new)
                .getChildNodes()
                .stream()
                .dropWhile(node -> Nodes.areDifferent(node, startingNode))
                .skip(1)
                .findFirst()
                .orElse(null);
        }

        private boolean isMethodCallViaVariable(ExpressionStmt subsequentStatement, SimpleName variableName)
        {
            return getFirstExpressionOfMethodCall(subsequentStatement) instanceof NameExpr methodnameExpr &&
                   methodnameExpr.getName()
                       .equals(variableName);
        }

        private @Nullable Node getFirstExpressionOfMethodCall(ExpressionStmt expressionStmt)
        {
            if (expressionStmt.getExpression() instanceof MethodCallExpr methodCall &&
                methodCall.getChildNodes()
                    .size() >= 2)
            {
                return methodCall.getChildNodes()
                    .get(0);
            }

            return null;
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
        return "@ExhaustiveSwitch must annotate variable initialized with a switch expression, followed by a method " +
               "call on it";
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
