package com.xcoder.easyinsta;

import org.jetbrains.annotations.NotNull;

public final class Task<T> {
    protected T value;
    protected Throwable exception;

    public Task(Exception e) {
        exception = e;
    }

    protected Task() {

    }

    public Task<T> addOnSuccessListener(OnSuccessListener<T> listener) {
        if (exception == null)
            listener.onSuccess(value);
        return this;
    }

    public Task<T> addOnFailureListener(OnFailureListener listener) {
        if (exception != null)
            listener.onFailed(exception);
        return this;
    }

    public void addOnCompleteListener(OnCompletionListener<T> callback) {
        callback.onComplete(this);
    }

    @NotNull
    public T getValue() {
        return value;
    }

    public boolean isSuccessful() {
        return exception == null;
    }

    public Throwable getException() {
        return exception;
    }

    public interface OnSuccessListener<T> {
        void onSuccess(T value);
    }

    public interface OnCompleteCallback {
        void onSuccess();

        void onFailed(Throwable e);
    }

    public interface OnCompletionListener<T> {
        void onComplete(Task<T> task);
    }

    public interface OnFailureListener {
        void onFailed(Throwable exception);
    }

}
