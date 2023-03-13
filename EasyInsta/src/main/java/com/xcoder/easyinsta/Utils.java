package com.xcoder.easyinsta;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.actions.users.UserAction;
import com.github.instagram4j.instagram4j.models.direct.IGThread;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadItem;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadTextItem;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.IGRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectInboxRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import com.xcoder.easyinsta.exceptions.InstagramException;
import com.xcoder.easyinsta.exceptions.Reasons;
import com.xcoder.easyinsta.interfaces.OnProgressListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import port.org.json.JSONException;
import port.org.json.JSONObject;

public class Utils {
    private static IGClient client;

    protected Utils(IGClient client) {
        Utils.client = client;
    }

    public <T extends IGResponse> T request(IGRequest<T> request) throws CompletionException, ExecutionException, InterruptedException, TimeoutException {
        return client.sendRequest(request).get(10, TimeUnit.SECONDS);
    }

    protected void followAction(String username, FriendshipsActionRequest.FriendshipsAction action) throws ExecutionException, InterruptedException, TimeoutException {
        long user = client.actions().users().findByUsername(username).get().getUser().getPk();
        request(new FriendshipsActionRequest(user, action));
    }


    protected UserAction getUser(String username) throws ExecutionException, InterruptedException, TimeoutException {
        return client.getActions().users().findByUsername(username).get(10,TimeUnit.SECONDS);
    }


    @NotNull
    protected IGThread getThread(String username) throws ExecutionException, InterruptedException {
        List<IGThread> threads = client.sendRequest(new DirectInboxRequest().limit(100)).get().getInbox().getThreads();
        for (IGThread thread : threads)
            for (Profile user : thread.getUsers()) {
                if (user.getUsername().equals(username)) {
                    return thread;
                }
            }
        throw new InstagramException("There is no user with username " + username, Reasons.INVALID_USERNAME);
    }


    protected List<ThreadItem> getThreadItem(String username, int count, boolean onlySent, @Nullable OnProgressListener listener) throws ExecutionException, InterruptedException {
        List<ThreadItem> list = new ArrayList<>();
        IGThread thread = getThread(username);
        String cursor = thread.getOldest_cursor();
        list.add(thread.getItems().get(0));
        if (count > 20) {
            int loop = (count / 20) + (count % 20 == 0 ? 0 : 1);
            for (int i = 0; i < loop; i++) {
                thread = client.sendRequest(new DirectThreadsRequest(thread.getThread_id(), cursor)).get().getThread();
                list.addAll(thread.getItems());
                cursor = thread.getOldest_cursor();
                if (listener != null)
                    listener.onProgress((i + 1) * 100 / loop);
                if (cursor == null)
                    break;
            }
        } else if (count == 0) {
            while (cursor != null) {
                thread = client.sendRequest(new DirectThreadsRequest(thread.getThread_id(), cursor)).get().getThread();
                list.addAll(thread.getItems());
                cursor = thread.getOldest_cursor();
            }
        } else {
            while (list.size() < count) {
                thread = client.sendRequest(new DirectThreadsRequest(thread.getThread_id(), cursor)).get().getThread();
                list.addAll(thread.getItems());
                cursor = thread.getOldest_cursor();
                if (cursor == null)
                    break;
            }
        }
        list.removeIf(threadItem -> onlySent && !isMessageSent(threadItem));
        return list.stream().limit(count == 0 ? list.size() : count).collect(Collectors.toList());
    }


    protected List<ThreadItem> getThreadItem(String username, String from, int frequency, boolean onlySent, @Nullable OnProgressListener listener) throws IllegalArgumentException, ExecutionException, InterruptedException {
        List<ThreadItem> items = getThreadItem(username, 0, onlySent, listener);
        List<String> messages = items
                .stream()
                .map(threadItem -> threadItem instanceof ThreadTextItem ? ((ThreadTextItem) threadItem).getText() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        if (frequency == 0) {
            frequency = Collections.frequency(messages, from);
        } else if (frequency < 0)
            throw new IllegalArgumentException("Frequency can't be negative");

        List<ThreadItem> threadItems = new ArrayList<>();
        for (ThreadItem item : items) {
            threadItems.add(item);
            if (item instanceof ThreadTextItem && ((ThreadTextItem) item).getText().equals(from)) {
                if (frequency == 1)
                    return threadItems;
                else
                    frequency--;
            }
        }
        throw new IllegalArgumentException("There is no such message '" + from + "' in the chat");
    }


    protected boolean isMessageSent(ThreadItem item) {
        return client.getSelfProfile().getPk() == item.getUser_id();
    }

    public static String[] extractUrls(String text) {
        List<String> containedUrls = new ArrayList<>();
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find()) {
            containedUrls.add(text.substring(urlMatcher.start(0), urlMatcher.end(0)));
        }
        return containedUrls.toArray(String[]::new);
    }

    @Deprecated
    public static String getDownloadURL(String link) {
        try {
            var url = new URL(link);
            if (url.getQuery() == null)
                link += "?__a=1&__d=dis";
            else
                link = link.replace(url.getQuery(), "__a=1&__d=dis");

            byte[] bytes = new URL(link).openStream().readAllBytes();
            JSONObject json = new JSONObject(new String(bytes));
            System.out.println(json.toString(3));
            if (url.getPath().contains("reel") || url.getPath().contains("p")){
                return json.getJSONArray("items")
                        .getJSONObject(0)
                        .getJSONArray("video_versions")
                        .getJSONObject(0)
                        .getString("url");
            } else {
                return json.getJSONObject("graphql")
                        .getJSONObject("user")
                        .getString("profile_pic_url_hd");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e){
            e.printStackTrace();
            throw new IllegalArgumentException("This link is not a valid instagram link");
        }
    }
}
