package com.example.libraries;

import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public final class FancyCounter1
{
    private final AtomicInteger value = new AtomicInteger(0);

    public int increase()
    {
        return value.incrementAndGet();
    }
}
