package dev.bannmann.mandor.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.mizool.core.exception.CodeInconsistencyException;

public class SourceBundle
{
    private final Map<Path, CompilationUnit> compilationUnits = new HashMap<>();

    public SourceBundle()
    {
        StaticJavaParser.getParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
            .setSymbolResolver(new JavaSymbolSolver(new NameOnlySolver()));
    }

    public SourceBundle importSources(String directory)
    {
        return importSources(Paths.get(directory));
    }

    public SourceBundle importSources(Path start)
    {
        try (Stream<Path> pathStream = Files.find(start, Integer.MAX_VALUE, this::isJavaSourceFile))
        {
            pathStream.map(this::parse)
                .forEach(this::add);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        return this;
    }

    private boolean isJavaSourceFile(Path filePath, BasicFileAttributes fileAttr)
    {
        return fileAttr.isRegularFile() &&
            filePath.getFileName()
                .toString()
                .endsWith(".java");
    }

    private CompilationUnit parse(Path path)
    {
        try
        {
            return parseFile(path);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private CompilationUnit parseFile(Path path) throws IOException
    {
        return StaticJavaParser.parse(path);
    }

    private void add(CompilationUnit compilationUnit)
    {
        compilationUnits.put(compilationUnit.getStorage()
            .orElseThrow(CodeInconsistencyException::new)
            .getPath(), compilationUnit);
    }

    public SourceBundle verify(SourceRule rule)
    {
        compilationUnits.values()
            .forEach(rule::scan);

        var violations = rule.getViolations();
        if (!violations.isEmpty())
        {
            throw new AssertionError("Rule '%s' was violated (%d times):%n%s".formatted(rule.getDescription(),
                violations.size(),
                String.join("\n", violations)));
        }

        return this;
    }
}
