package dev.bannmann.mandor.core;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.mizool.core.exception.CodeInconsistencyException;
import dev.bannmann.labs.core.Nullness;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class RuleContext
{
    @Getter
    private final TypeSolver typeSolver;

    private @Nullable CompilationUnit compilationUnit;

    private @Nullable Path relativePath;

    private final Function<Path, Stream<CompilationUnit>> lookup;

    void activate(CompilationUnit compilationUnit, Path relativePath)
    {
        this.compilationUnit = compilationUnit;
        this.relativePath = relativePath;
    }

    public CompilationUnit getCompilationUnit()
    {
        if (compilationUnit == null)
        {
            throw new IllegalStateException();
        }
        return compilationUnit;
    }

    public Path getRelativePath()
    {
        if (relativePath == null)
        {
            throw new IllegalStateException();
        }
        return relativePath;
    }

    public Path getFilePath()
    {
        return getCompilationUnit().getStorage()
            .map(CompilationUnit.Storage::getPath)
            .orElseThrow(IllegalStateException::new);
    }

    public String getCodeLocation(Node startingNode)
    {
        return "(%s:%s)".formatted(getFilePath().getFileName(),
            startingNode.getRange()
                .map(range -> range.begin.line)
                .orElseThrow(CodeInconsistencyException::new));
    }

    /**
     * @return the compilation units residing at the given relative path. May be empty, but never {@code null}.
     */
    public Stream<CompilationUnit> lookup(Path path)
    {
        return lookup.apply(path);
    }

    /**
     * @return the package-info files for the package of the current compilation unit. There may be as many as one per directory imported into the {@link SourceBundle}.
     */
    public Stream<PackageDeclaration> getPackageInfoFiles()
    {
        Path directoryPath = Nullness.guaranteeNonNull(getRelativePath().getParent());
        Path packageInfoPath = directoryPath.resolve("package-info.java");

        return lookup(packageInfoPath).flatMap(packageInfoUnit -> packageInfoUnit.getPackageDeclaration()
            .stream());
    }
}
