package com.xcoder.easyinsta;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.actions.users.UserAction;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.IGRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

class Utils {
    private static IGClient client;
    private Object meta;
    protected Utils(IGClient client){
        Utils.client = client;
    }

    protected <T extends IGResponse> Task<String> request(IGRequest<T> request) throws CompletionException {
        Task<String> task = new Task<>();
        CompletableFuture<T> response = client.sendRequest(request);
        response.thenAccept(new Consumer<IGResponse>() {
            @Override
            public void accept(IGResponse igResponse) {
               task.value = igResponse.getMessage();
            }
        }).exceptionally(new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable throwable) {
                task.exception = throwable;
                return null;
            }
        }).join();
        return task;
    }


    protected byte[] readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);
        return bytes;
    }


    protected Task<String> followAction(String username, FriendshipsActionRequest.FriendshipsAction action){
        try {
            User user = client.actions().users().findByUsername(username).get().getUser();
            return request(new FriendshipsActionRequest(user.getPk(),action));
        } catch (ExecutionException | InterruptedException e) {
            return new Task<>(e);
        }
    }


    protected Object getProfileMetadata(CompletableFuture<UserAction> response, String what){
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
                        default:
                            meta = userAction.getUser().getMedia_count();
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
        } catch (CompletionException e){
            return e;
        }
    }

    protected Task<List<String>> getFeeds(CompletableFuture<UserAction> response,boolean isFollowersFeed){
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
        } catch (CompletionException e){
            return new Task<>(e);
        }
    }
}
