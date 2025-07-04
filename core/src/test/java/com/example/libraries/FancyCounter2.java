package com.example.libraries;

import java.util.concurrent.atomic.AtomicInteger;

@net.jcip.annotations.ThreadSafe
public final class FancyCounter2
{
    private final AtomicInteger value = new AtomicInteger(0);

    public int increase()
    {
        return value.incrementAndGet();
    }
}
