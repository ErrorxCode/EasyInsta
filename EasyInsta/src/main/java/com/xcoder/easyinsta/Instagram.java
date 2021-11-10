package com.xcoder.easyinsta;

import android.os.Build;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.exceptions.IGLoginException;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsBroadcastRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsPendingRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.accounts.LoginResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import com.github.instagram4j.instagram4j.responses.media.MediaResponse;
import com.github.instagram4j.instagram4j.utils.IGChallengeUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is an easy implementation of instagram private api in java. This class include various methods to perform instagram operation like
 * direct messages,adding stories,posting photos,scrapping profile data like posts,followers,followings etc.
 * 
 */
public class Instagram {
    private final IGClient client;
    private Utils utils;

    /**
     * Loges in to your instagram account with the provided username and password. If your account has two-factor authentication,
     * then use second constructor which takes a {@link Callable<String>} as an argument.
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     */
    public Instagram(@NotNull String username, @NotNull String password) {
        try {
            client = IGClient.builder().username(username).password(password).login();
            utils = new Utils(client);
        } catch (IGLoginException e) {
            e.printStackTrace();
            if (e.getLoginResponse().getTwo_factor_info() != null) {
                throw new InstagramException("This account requires 2-factor-authentication. Use second constructor for type of login passing a callback as third argument.", Reasons.REQUIRE_2_FACTOR_AUTHENTICATION);
            } else
                throw new InstagramException(e.getMessage(), Reasons.LOGIN_ERROR_UNKNOWN);
        }
    }


    /**
     * Loges in to your instagram account with the provided username and password and then authenticate using two-factor-authentication.
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     * @param callback The callback that will be invoked when verification code is sent. You have to return that code.
     */
    public Instagram(@NotNull String username, @NotNull String password, @NotNull Callable<String> callback) {
        try {
            client = IGClient.builder()
                    .username(username)
                    .password(password)
                    .onTwoFactor(new IGClient.Builder.LoginHandler() {
                        @Override
                        public LoginResponse accept(IGClient client, LoginResponse t) {
                            return IGChallengeUtils.resolveTwoFactor(client, t, callback);
                        }
                    }).login();
        } catch (IGLoginException e) {
            e.printStackTrace();
            if (e.getLoginResponse().getTwo_factor_info() != null) {
                throw new InstagramException("This account requires 2-factor-authentication. Use second constructor for type of login passing a callback as third argument.", Reasons.REQUIRE_2_FACTOR_AUTHENTICATION);
            } else
                throw new InstagramException(e.getMessage(), Reasons.LOGIN_ERROR_UNKNOWN);
        }
    }


    /**
     * Sends a direct message to a user
     * @param username The username of the user.
     * @param message The text to send.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void directMessage(@NotNull String username, @NotNull String message, @Nullable OnCompleteCallback callback) {
        try {
            User user = client.actions().users().findByUsername(username).get().getUser();
            DirectThreadsBroadcastRequest.BroadcastTextPayload payload = new DirectThreadsBroadcastRequest.BroadcastTextPayload(message, user.getPk());
            utils.request(new DirectThreadsBroadcastRequest(payload), callback);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            if (callback != null)
                callback.onFailed(e);
        }
    }


    /**
     * Sends a photo as a direct message to the user.
     * @param username The username of the user.
     * @param photo The photo to send.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void directMessage(@NotNull String username, @NotNull File photo, @Nullable OnCompleteCallback callback) {
        if (photo.getName().endsWith(".mp4"))
            throw new InstagramException("Uploading or sending video is not currently supported",Reasons.UNSUPPORTED_FILE_FORMAT);
        try {
            User user = client.actions().users().findByUsername(username).get().getUser();
            DirectThreadsBroadcastRequest.BroadcastPayload payload = new DirectThreadsBroadcastRequest.BroadcastConfigurePhotoPayload(photo.getName(), user.getPk());
            utils.request(new DirectThreadsBroadcastRequest(payload), callback);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            if (callback != null)
                callback.onFailed(e);
        }
    }

    /**
     * Follows the user.
     * @param username Username of the user to follow.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void follow(@NotNull String username, @Nullable OnCompleteCallback callback) {
        utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.CREATE, callback);
    }


    /**
     * Unfollows the user.
     * @param username Username of the user to unfollow.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void unfollow(@NotNull String username, @Nullable OnCompleteCallback callback) {
        utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.DESTROY, callback);
    }


    /**
     * Accept the pending follow request, if had any.
     * @param username Username of the user who had requested.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void accept(@NotNull String username, @Nullable OnCompleteCallback callback) {
        utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.APPROVE, callback);
    }


    /**
     * Ignores the pending follow request, if had any.
     * @param username Username of the user who had requested.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void ignore(@NotNull String username, @Nullable OnCompleteCallback callback) {
        utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.IGNORE, callback);
    }


    /**
     * Remove the user from your followers.
     * @param username Username of the user to remove.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void removeFollower(@NotNull String username, @Nullable OnCompleteCallback callback) {
        utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.REMOVE_FOLLOWER, callback);
    }


    /**
     * Post a new picture.
     * @param photo The photo to post
     * @param caption Caption for the post
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void post(@NotNull File photo, @Nullable String caption, @Nullable OnCompleteCallback callback) {
        if (photo.getName().endsWith(".mp4"))
            throw new InstagramException("Uploading or sending video is not currently supported",Reasons.UNSUPPORTED_FILE_FORMAT);
        CompletableFuture<MediaResponse.MediaConfigureTimelineResponse> response = client.actions().timeline().uploadPhoto(photo, caption == null ? "Uploaded using EasyInsta library" : caption);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && callback != null) {
            response.thenAccept(new Consumer<MediaResponse.MediaConfigureTimelineResponse>() {
                @Override
                public void accept(MediaResponse.MediaConfigureTimelineResponse response) {
                    callback.onSuccess();
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    callback.onFailed(throwable);
                    return null;
                }
            });
        } else {
            if (callback != null) {
                try {
                    response.get();
                    callback.onSuccess();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    callback.onFailed(e);
                }
            }
        }
    }


    /**
     * Changes the profile picture. Currently not working...
     * @param pic The photo to replace
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    private void setDp(@NotNull File pic, @Nullable OnCompleteCallback callback) {
        // ---------------- ! Not working !----------------------
        throw new UnsupportedOperationException("Don't try to be over smart");
    }


    /**
     * Sets the bio of your profile
     * @param bio The bio to set.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void setBio(@NotNull String bio, @Nullable OnCompleteCallback callback) {
        if (bio.length() > 150)
            throw new InstagramException("Bio must be of less then 150 characters", Reasons.BIO_LENGTH_EXCEEDED);
        CompletableFuture<IGResponse> response = client.actions().account().setBio(bio);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && callback != null) {
            response.thenAccept(new Consumer<IGResponse>() {
                @Override
                public void accept(IGResponse response) {
                    callback.onSuccess();
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    callback.onFailed(throwable);
                    return null;
                }
            });
        } else {
            if (callback != null) {
                try {
                    response.get();
                    callback.onSuccess();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    callback.onFailed(e);
                }
            }
        }
    }


    /**
     * Adds a photo to your story.
     * @param photo The photo to add as story.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     */
    public void addStory(@NotNull File photo, @Nullable OnCompleteCallback callback) {
        if (photo.getName().endsWith(".mp4"))
            throw new InstagramException("Uploading or sending video is not currently supported",Reasons.UNSUPPORTED_FILE_FORMAT);
        CompletableFuture<MediaResponse.MediaConfigureToStoryResponse> response = client.actions().story().uploadPhoto(photo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && callback != null) {
            response.thenAccept(new Consumer<MediaResponse.MediaConfigureToStoryResponse>() {
                @Override
                public void accept(MediaResponse.MediaConfigureToStoryResponse response) {
                    callback.onSuccess();
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    callback.onFailed(throwable);
                    return null;
                }
            });
        } else {
            if (callback != null) {
                try {
                    response.get();
                    callback.onSuccess();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    callback.onFailed(e);
                }
            }
        }
    }


    /**
     * Scraps followers of the account associated with the provided username. Empty list if none found but Never null.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @param username The username of the account from which you want to scrap.
     * @return The usernames of the followers.
     */
    public @NotNull List<String> getFollowers(@NotNull String username, @Nullable OnCompleteCallback callback) {
        return utils.getFeeds(client.actions().users().findByUsername(username),true,callback);
    }


    /**
     * Scraps followings of the account associated with the provided username. Empty list if none found but Never null.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @param username The username of the account from which you want to scrap.
     * @return The usernames of the followings.
     */
    public @NotNull List<String> getFollowings(@NotNull String username, @Nullable OnCompleteCallback callback) {
        return utils.getFeeds(client.actions().users().findByUsername(username),false,callback);
    }


    /**
     * Gets the pending follow requests. Empty list if none found but Never null.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @return List of the usernames.
     */
    public @NotNull List<String> getFollowRequests(@Nullable OnCompleteCallback callback) {
        List<String> usernames = new ArrayList<>();
        CompletableFuture<FeedUsersResponse> response = client.sendRequest(new FriendshipsPendingRequest());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && callback != null) {
            response.thenAccept(new Consumer<FeedUsersResponse>() {
                @Override
                public void accept(FeedUsersResponse feedUsersResponse) {
                    for (Profile user : feedUsersResponse.getUsers()) {
                        usernames.add(user.getUsername());
                    }
                    callback.onSuccess();
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
                for (Profile user : response.get().getUsers())
                    usernames.add(user.getUsername());
                if (callback != null)
                    callback.onSuccess();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                if (callback != null)
                    callback.onFailed(e);
            }
        }
        return usernames;
    }


    /**
     * Gets the bio of the account associated with the username provided.
     * @param username The username of the account.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @return Bio of the user
     */
    public @Nullable String getBio(@NotNull String username, @Nullable OnCompleteCallback callback) {
        return utils.getProfileMetadata(client.actions().users().findByUsername(username),"bio",callback);
    }


    /**
     * Gets the profile photo link of the account associated with the username provided.
     * @param username The username of the account.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @return link of the profile pic
     */
    public @Nullable String getProfilePicUrl(@NotNull String username, @Nullable OnCompleteCallback callback) {
        return utils.getProfileMetadata(client.actions().users().findByUsername(username),"dp",callback);
    }


    /**
     * Gets the follower count of the account associated with the username provided.
     * @param username The username of the account.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @return No. of followers
     */
    public int getFollowersCount(@NotNull String username, @Nullable OnCompleteCallback callback) {
        return Integer.parseInt(utils.getProfileMetadata(client.actions().users().findByUsername(username),"followers",callback));
    }


    /**
     * Gets the followings count of the account associated with the username provided.
     * @param username The username of the account.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @return No. of followings
     */
    public int getFollowingsCount(@NotNull String username, @Nullable OnCompleteCallback callback) {
        return Integer.parseInt(utils.getProfileMetadata(client.actions().users().findByUsername(username),"followings",callback));
    }


    /**
     * Gets the post count of the account associated with the username provided.
     * @param username The username of the account.
     * @param callback Callback that shows the success or failure of the request. This is optional (Nullable)
     * @return No. of posts
     */
    public int getPostCount(@NotNull String username, @Nullable OnCompleteCallback callback) {
        return Integer.parseInt(utils.getProfileMetadata(client.actions().users().findByUsername(username),"post",callback));
    }
}
