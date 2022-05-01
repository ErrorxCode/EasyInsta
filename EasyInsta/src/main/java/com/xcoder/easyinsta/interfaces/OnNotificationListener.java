package com.xcoder.easyinsta.interfaces;

import com.xcoder.easyinsta.Message;

import org.jetbrains.annotations.NotNull;

public interface OnNotificationListener {
    void onMessage(@NotNull Message message);
}
