package com.example.libraries;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class SimpleCounter1
{
    private int value;

    public int increase()
    {
        return ++value;
    }
}
