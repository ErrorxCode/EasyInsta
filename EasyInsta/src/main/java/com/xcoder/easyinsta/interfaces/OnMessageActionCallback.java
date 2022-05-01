package com.xcoder.easyinsta.interfaces;

import org.jetbrains.annotations.Nullable;

/**
     * Interface for handling callbacks of spam messages.
     */
    public interface OnMessageActionCallback {
        void onSuccess(@Nullable String message);

        void onFailed(Exception exception);

        void onProgress(int percentage);
    }