package dev.bannmann.mandor.core.rules;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.mandor.core.AbstractSourceVisitor;
import dev.bannmann.mandor.core.Context;
import dev.bannmann.mandor.core.SourceRule;
import dev.bannmann.mandor.core.util.Nodes;

public class MalformedExhaustiveSwitch extends SourceRule
{
    private static class Visitor extends AbstractSourceVisitor
    {
        @Override
        public void visit(MarkerAnnotationExpr markerAnnotation, Context context)
        {
            super.visit(markerAnnotation, context);
            process(markerAnnotation);
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
                    getContext().getEnclosingTypeName(annotation),
                    getContext().getFileLocation(annotation));
            }
        }

        private boolean isUsedCorrectly(AnnotationExpr annotation)
        {
            Node assignmentNode = annotation.getParentNode()
                .orElseThrow(CodeInconsistencyException::new);

            List<Node> nodes = assignmentNode.getChildNodes();
            if (nodes.size() != 2 ||
                Nodes.nodesAreDifferent(nodes.get(0), annotation) ||
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
                .dropWhile(node -> Nodes.nodesAreDifferent(node, startingNode))
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
        return "@ExhaustiveSwitch must annotate variable initialized with a switch expression, followed by a method " +
               "call on it";
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
