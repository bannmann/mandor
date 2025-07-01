package dev.bannmann.mandor.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.concurrent.NotThreadSafe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.errorprone.annotations.MustBeClosed;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@NotThreadSafe
class MemoStack<T>
{
    public interface MemoHandle extends AutoCloseable
    {
        @Override
        void close();
    }

    @RequiredArgsConstructor
    private static class Memo<T>
    {
        @SuppressWarnings("java:S3985")
        @SuppressWarningsRationale("Sonar thinks this class is unused")
        private class HandleImpl implements MemoHandle
        {
            @Override
            public void close()
            {
                closer.accept(Memo.this);
            }
        }

        @Getter
        private final T contents;

        private final Consumer<Memo<?>> closer;

        @Getter
        private final Memo<T>.HandleImpl handle = new Memo<T>.HandleImpl();
    }

    private final Deque<Memo<T>> memos = new ArrayDeque<>();

    @MustBeClosed
    public MemoHandle create(T contents)
    {
        Memo<T> memo = new Memo<>(contents, this::remove);
        memos.addLast(memo);
        return memo.getHandle();
    }

    private void remove(Memo<?> stackableMemo)
    {
        if (memos.peekLast() != stackableMemo)
        {
            throw new IllegalStateException("Attempt to close memo that is not the last to be created");
        }
        memos.removeLast();
    }

    public Optional<T> accessLastMemoContents()
    {
        return Optional.ofNullable(memos.peek())
            .map(Memo::getContents);
    }
}
