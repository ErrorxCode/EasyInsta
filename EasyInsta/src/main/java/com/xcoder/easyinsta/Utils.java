package com.xcoder.easyinsta;

import android.os.Build;

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
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

class Utils {
    private static IGClient client;
    private String string;
    protected Utils(IGClient client){
        Utils.client = client;
    }

    protected <T extends IGResponse> void request(IGRequest<T> request, OnCompleteCallback callback) {
        CompletableFuture<T> response = client.sendRequest(request);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && callback != null) {
            response.thenAccept(new Consumer<IGResponse>() {
                @Override
                public void accept(IGResponse igResponse) {
                    callback.onSuccess();
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    callback.onFailed(throwable);
                    return null;
                }
            });
        }
    }


    protected byte[] readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);
        return bytes;
    }


    protected void followAction(String username, FriendshipsActionRequest.FriendshipsAction action, OnCompleteCallback callback){
        try {
            User user = client.actions().users().findByUsername(username).get().getUser();
            request(new FriendshipsActionRequest(user.getPk(),action), callback);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            if (callback == null)
                followAction(username,action, null);
            else
                callback.onFailed(e);
        }
    }

    protected String getProfileMetadata(CompletableFuture<UserAction> response,String what,OnCompleteCallback callback){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && callback != null) {
            response.thenAccept(new Consumer<UserAction>() {
                @Override
                public void accept(UserAction userAction) {
                    callback.onSuccess();
                    switch (what) {
                        case "dp":
                            string = userAction.getUser().getProfile_pic_url();
                            break;
                        case "bio":
                            string = userAction.getUser().getBiography();
                            break;
                        case "followers":
                            string = String.valueOf(userAction.getUser().getFollower_count());
                            break;
                        case "followings":
                            string = String.valueOf(userAction.getUser().getFollowing_count());
                            break;
                        default:
                            string = String.valueOf(userAction.getUser().getMedia_count());
                            break;
                    }
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    callback.onFailed(throwable);
                    return null;
                }
            }).join();
        } else {
            try {
                switch (what) {
                    case "dp":
                        string = response.get().getUser().getProfile_pic_url();
                        break;
                    case "bio":
                        string = response.get().getUser().getBiography();
                        break;
                    case "followers":
                        string = String.valueOf(response.get().getUser().getFollower_count());
                        break;
                    case "followings":
                        string = String.valueOf(response.get().getUser().getFollowing_count());
                        break;
                    default:
                        string = String.valueOf(response.get().getUser().getMedia_count());
                        break;
                }
                if (callback != null)
                    callback.onSuccess();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                if (callback != null)
                    callback.onFailed(e);
            }
        }
        return string;
    }

    protected List<String> getFeeds(CompletableFuture<UserAction> response,boolean isFollowersFeed,OnCompleteCallback callback){
        List<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && callback != null) {
            response.thenAccept(new Consumer<UserAction>() {
                @Override
                public void accept(UserAction userAction) {
                    callback.onSuccess();
                    for (FeedUsersResponse feed : isFollowersFeed ? userAction.followersFeed() : userAction.followingFeed()) {
                        for (Profile user : feed.getUsers()) {
                            list.add(user.getUsername());
                        }
                    }
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    callback.onFailed(throwable);
                    return null;
                }
            }).join();
        } else {
            try {
                for (FeedUsersResponse feed : isFollowersFeed ? response.get().followersFeed() : response.get().followingFeed()) {
                    for (Profile user : feed.getUsers()) {
                        list.add(user.getUsername());
                    }
                }
                if (callback != null)
                    callback.onSuccess();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                if (callback != null)
                    callback.onFailed(e);
            }
        }
        return list;
    }
}
