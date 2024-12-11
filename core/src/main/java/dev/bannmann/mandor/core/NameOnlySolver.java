package dev.bannmann.mandor.core;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

@RequiredArgsConstructor
class NameOnlySolver implements TypeSolver
{
    private @Nullable TypeSolver parent;

    @Override
    public @Nullable TypeSolver getParent()
    {
        return parent;
    }

    @Override
    public void setParent(@Nullable TypeSolver parent)
    {
        this.parent = parent;
    }

    @Override
    public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name)
    {
        return SymbolReference.solved(fakeDeclaration(name));
    }

    private ResolvedAnnotationDeclaration fakeDeclaration(String name)
    {
        return new ResolvedAnnotationDeclaration()
        {
            @Override
            public List<ResolvedTypeParameterDeclaration> getTypeParameters()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getName()
            {
                return name;
            }

            @Override
            public Optional<ResolvedReferenceTypeDeclaration> containerType()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getPackageName()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getClassName()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getQualifiedName()
            {
                return name;
            }

            @Override
            public List<ResolvedReferenceType> getAncestors(boolean acceptIncompleteList)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<ResolvedFieldDeclaration> getAllFields()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<ResolvedMethodDeclaration> getDeclaredMethods()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<MethodUsage> getAllMethods()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isAssignableBy(ResolvedType type)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isAssignableBy(ResolvedReferenceTypeDeclaration other)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasDirectlyAnnotation(String qualifiedName)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isFunctionalInterface()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<ResolvedConstructorDeclaration> getConstructors()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<ResolvedAnnotationMemberDeclaration> getAnnotationMembers()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isInheritable()
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
