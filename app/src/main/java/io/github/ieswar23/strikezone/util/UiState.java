package io.github.ieswar23.strikezone.util;

import androidx.annotation.Nullable;

/** Loading / content / empty / error wrapper for screen state. */
public final class UiState<T> {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR }

    private final Status status;
    @Nullable
    private final T data;
    @Nullable
    private final String message;

    private UiState(Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> UiState<T> loading(@Nullable T previous) {
        return new UiState<>(Status.LOADING, previous, null);
    }

    public static <T> UiState<T> content(T data) {
        return new UiState<>(Status.CONTENT, data, null);
    }

    public static <T> UiState<T> empty() {
        return new UiState<>(Status.EMPTY, null, null);
    }

    public static <T> UiState<T> error(String message, @Nullable T previous) {
        return new UiState<>(Status.ERROR, previous, message);
    }

    public Status getStatus() {
        return status;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }
}
