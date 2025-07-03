package dev.bannmann.mandor.core.rules;

import org.kohsuke.MetaInfServices;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import dev.bannmann.mandor.core.Nodes;
import dev.bannmann.mandor.core.SourceRule;

@MetaInfServices
public class OctalNumberUsage extends SourceRule
{
    private class Visitor extends VoidVisitorAdapter<Void>
    {
        private final OctalDetector octalDetector = new OctalDetector();

        @Override
        public void visit(IntegerLiteralExpr node, Void arg)
        {
            super.visit(node, arg);

            process(node);
        }

        private void process(LiteralStringValueExpr node)
        {
            if (octalDetector.isOctal(node.getValue()))
            {
                addViolation("%s contains an octal number literal in %s",
                    Nodes.getEnclosingTypeName(node),
                    getContext().getCodeLocation(node));
            }
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
        return "Number literals should not use octal notation";
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
