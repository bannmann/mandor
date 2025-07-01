package dev.bannmann.mandor.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.github.javaparser.ast.expr.AnnotationExpr;

@RequiredArgsConstructor
class Suppression
{
    @Getter
    private final AnnotationExpr annotationExpr;

    private boolean hit;

    public void trackHit()
    {
        this.hit = true;
    }

    public boolean wasHit()
    {
        return this.hit;
    }
}
