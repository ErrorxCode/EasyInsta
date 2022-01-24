package com.xcoder.easyinsta;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.actions.users.UserAction;
import com.github.instagram4j.instagram4j.models.direct.IGThread;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadItem;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadTextItem;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.IGRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectInboxRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedTimelineRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.media.MediaInfoRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedTimelineResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import com.xcoder.easyinsta.exceptions.InstagramException;
import com.xcoder.easyinsta.exceptions.Reasons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class Utils {
    private static IGClient client;
    private Object meta;


    protected Utils(IGClient client) {
        Utils.client = client;
    }

    public <T extends IGResponse> Task<Void> request(IGRequest<T> request) throws CompletionException {
        Task<Void> task = new Task<>();
        client.sendRequest(request).exceptionally(new Function<Throwable, T>() {
            @Override
            public T apply(Throwable throwable) {
                task.exception = throwable;
                return null;
            }
        }).join();
        return task;
    }


    byte[] readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);
        return bytes;
    }


    protected Task<Void> followAction(String username, FriendshipsActionRequest.FriendshipsAction action) {
        try {
            User user = client.actions().users().findByUsername(username).get().getUser();
            return request(new FriendshipsActionRequest(user.getPk(), action));
        } catch (ExecutionException | InterruptedException e) {
            return new Task<>(e);
        }
    }


    protected Object getProfileMetadata(CompletableFuture<UserAction> response, String what) {
        try {
            response.thenAccept(new Consumer<UserAction>() {
                @Override
                public void accept(UserAction userAction) {
                    switch (what) {
                        case "dp":
                            meta = userAction.getUser().getProfile_pic_url();
                            break;
                        case "bio":
                            meta = userAction.getUser().getBiography();
                            break;
                        case "followers":
                            meta = userAction.getUser().getFollower_count();
                            break;
                        case "followings":
                            meta = userAction.getUser().getFollowing_count();
                            break;
                        case "posts":
                            meta = userAction.getUser().getMedia_count();
                            break;
                    }
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    meta = throwable;
                    return null;
                }
            }).join();
            return meta;
        } catch (CompletionException e) {
            return e;
        }
    }


    protected Task<List<String>> getFeeds(CompletableFuture<UserAction> response, boolean isFollowersFeed) {
        Task<List<String>> task = new Task<>();
        List<String> list = new ArrayList<>();
        try {
            response.thenAccept(new Consumer<UserAction>() {
                @Override
                public void accept(UserAction userAction) {
                    for (FeedUsersResponse feed : isFollowersFeed ? userAction.followersFeed() : userAction.followingFeed()) {
                        for (Profile user : feed.getUsers()) {
                            list.add(user.getUsername());
                        }
                    }
                    task.value = list;
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    task.exception = throwable;
                    return null;
                }
            }).join();
            return task;
        } catch (CompletionException e) {
            return new Task<>(e);
        }
    }

    @NotNull
    protected IGThread getThread(String username) throws ExecutionException, InterruptedException {
        List<IGThread> threads = client.sendRequest(new DirectInboxRequest()).get().getInbox().getThreads();
        for (IGThread thread : threads)
            for (Profile user : thread.getUsers()) {
                if (user.getUsername().equals(username)) {
                    return thread;
                }
            }
        throw new InstagramException("There is no user with username " + username, Reasons.INVALID_USERNAME);
    }


    protected List<ThreadItem> getThreadItem(String username, int count, boolean onlySent, @Nullable Instagram.OnProgressListener listener) throws ExecutionException, InterruptedException {
        List<ThreadItem> list = new ArrayList<>();
        IGThread thread = getThread(username);
        String cursor = thread.getOldest_cursor();
        list.add(thread.getItems().get(0));
        if (count > 20) {
            int loop = (count/20) + (count%20 == 0 ? 0 : 1);
            for (int i = 0; i < loop; i++){
                thread = client.sendRequest(new DirectThreadsRequest(thread.getThread_id(), cursor)).get().getThread();
                list.addAll(thread.getItems());
                cursor = thread.getOldest_cursor();
                if (listener != null)
                    listener.onProgress((i + 1)*100/loop);
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


    protected List<ThreadItem> getThreadItem(String username, String from, int frequency, boolean onlySent, @Nullable Instagram.OnProgressListener listener) throws IllegalArgumentException, ExecutionException, InterruptedException {
        List<ThreadItem> items = getThreadItem(username, 0, onlySent,listener);
        List<String> messages = items
                .stream()
                .map(threadItem -> threadItem instanceof ThreadTextItem ? ((ThreadTextItem) threadItem).getText() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        if (frequency == 0){
            frequency = Collections.frequency(messages,from);
        } else if (frequency < 0)
            throw new IllegalArgumentException("Frequency can't be negative");

        List<ThreadItem> threadItems = new ArrayList<>();
        for (ThreadItem item : items) {
            threadItems.add(item);
            if (item instanceof ThreadTextItem && ((ThreadTextItem) item).getText().equals(from)){
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
}
