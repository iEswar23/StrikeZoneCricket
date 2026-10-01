package io.github.ieswar23.strikezone.util;

/** Status of the catalogue sync with the (mock) tournament API. */
public final class SyncState {
    public enum Status { IDLE, SYNCING, SYNCED, FAILED }

    private final Status status;
    private final String message;

    private SyncState(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static SyncState idle() {
        return new SyncState(Status.IDLE, null);
    }

    public static SyncState syncing() {
        return new SyncState(Status.SYNCING, null);
    }

    public static SyncState synced() {
        return new SyncState(Status.SYNCED, null);
    }

    public static SyncState failed(String message) {
        return new SyncState(Status.FAILED, message);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
