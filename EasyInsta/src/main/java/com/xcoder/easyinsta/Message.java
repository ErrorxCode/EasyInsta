package com.xcoder.easyinsta;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsBroadcastRequest;
import com.xcoder.easyinsta.interfaces.OnNotificationListener;
import com.xcoder.easyinsta.interfaces.OnProgressListener;
import com.xcoder.tasks.AsyncTask;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Message that is received when a new direct message is received. This class contains info about the new message.
 */
public final class Message {
    private final IGClient client;
    public final String text;
    public final String threadId;
    public final String itemId;
    public final long userId;

    public Message(IGClient client, String text, String threadId, String itemId, long userId) {
        this.client = client;
        this.text = text;
        this.threadId = threadId;
        this.itemId = itemId;
        this.userId = userId;
    }

    public void reply(String message) {
        client.sendRequest(new DirectThreadsBroadcastRequest(new DirectThreadsBroadcastRequest.BroadcastTextPayload(message, threadId)));
    }

    @Nullable
    public String getSenderUsername() {
        try {
            return client.actions().users().info(userId).get(5, TimeUnit.SECONDS).getUsername();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return null;
        }
    }
}