package com.example.nullness;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class OutsideUse<T extends @Nullable Object> // Violation: NullabilityAnnotationOutsideNullMarkedCode
{
    private @Nullable List<String> whatever; // Violation: NullabilityAnnotationOutsideNullMarkedCode

    public @NonNull T check(T t) // Violation: NullabilityAnnotationOutsideNullMarkedCode
    {
        if (t == null)
        {
            throw new IllegalArgumentException();
        }
        return t;
    }
}
