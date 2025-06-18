package com.example.nullness;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullMarkedClass
{
    public static class Nested
    {
        private @Nullable Object object; // No violation of NullabilityAnnotationOutsideNullMarkedCode
    }

    public @Nullable String doStuff() // No violation of NullabilityAnnotationOutsideNullMarkedCode
    {
        return null;
    }
}
