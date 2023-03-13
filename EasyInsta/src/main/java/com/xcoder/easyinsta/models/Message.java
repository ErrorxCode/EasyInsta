package com.xcoder.easyinsta.models;

import com.xcoder.easyinsta.Instagram;
import com.xcoder.tasks.AsyncTask;

/**
 * Message that is received when a new direct message is received. This class contains info about the new message.
 */
public final class Message {
    public final Instagram insta;
    public final String text;
    public final String sender;

    public Message(Instagram insta, String text, String sender) {
        this.insta = insta;
        this.text = text;
        this.sender = sender;
    }

    public AsyncTask<Void> reply(String message) {
        return insta.direct().directMessage(sender,message);
    }

    @Override
    public String toString() {
        return "Message{" +
                ", text='" + text + '\'' +
                ", sender='" + sender + '\'' +
                '}';
    }
}