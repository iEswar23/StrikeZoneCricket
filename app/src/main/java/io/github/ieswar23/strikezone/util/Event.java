package io.github.ieswar23.strikezone.util;

import androidx.annotation.Nullable;

/** One-shot LiveData payload (navigation, popups) that is consumed only once. */
public class Event<T> {
    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    @Nullable
    public T consume() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public T peek() {
        return content;
    }

    public boolean isHandled() {
        return handled;
    }
}
