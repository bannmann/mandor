package com.example.nullness.null_marked;

import java.math.BigDecimal;

import lombok.NonNull;

public class LombokUse
{
    // No violation of UndesiredNullabilityAnnotation: Lombok @NonNull is fine
    public void method(@NonNull BigDecimal value)
    {
        System.out.println(value);
    }
}
