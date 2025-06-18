package com.example.nullness.null_marked;

import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.example.nullness.other.Nullable;

public class UseOfUndesiredAnnotation
{
    private @Nullable List<String> foo; // Violation: UndesiredNullabilityAnnotation

    private List<@NonNull String> bar; // Violation: UndesiredNullabilityAnnotation
}
