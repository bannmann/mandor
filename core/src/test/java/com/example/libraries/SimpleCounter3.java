package com.example.libraries;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public final class SimpleCounter3
{
    private int value;

    public int increase()
    {
        return ++value;
    }
}
