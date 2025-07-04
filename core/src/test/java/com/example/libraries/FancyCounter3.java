package com.example.libraries;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.errorprone.annotations.ThreadSafe;

@ThreadSafe
public final class FancyCounter3
{
    private final AtomicInteger value = new AtomicInteger(0);

    public int increase()
    {
        return value.incrementAndGet();
    }
}
