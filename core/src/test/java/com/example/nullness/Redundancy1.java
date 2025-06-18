package com.example.nullness;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class Redundancy1
{
    @NullMarked // Violation: RedundantlyNullMarkedCode
    public Object get()
    {
        return new Object();
    }
}
