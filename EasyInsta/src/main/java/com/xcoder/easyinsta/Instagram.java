package com.xcoder.easyinsta;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsBroadcastRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserStoryRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsPendingRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.accounts.LoginResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserStoryResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import com.github.instagram4j.instagram4j.responses.media.MediaResponse;
import com.github.instagram4j.instagram4j.utils.IGChallengeUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import okhttp3.OkHttpClient;

/**
 * This is an easy implementation of instagram private api in java. This class include various methods to perform instagram operation like
 * direct messages,adding stories,posting photos,scrapping profile data like posts,followers,followings etc.
 * @author Rahil khan
 * @version 1.0
 */
public class Instagram {
    private static IGClient client;
    private Utils utils;


    private Instagram(IGClient cachedClient){
        client = cachedClient;
        utils = new Utils(client);
    }

    /**
     * Loges in to your instagram account with the provided username and password. If your account has two-factor authentication,
     * then use second constructor which takes a {@link Callable<String>} as an argument.
     * @param username Username of the account.
     * @param password Password of the account.
     */
    public Instagram(@NotNull String username, @NotNull String password) throws IGLoginException{
        this(username,password, (InetSocketAddress) null);
    }

    /**
     * Loges in to your instagram account with the provided username and password. If your account has two-factor authentication,
     * then use second constructor which takes a {@link Callable<String>} as an argument.
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     * @param proxy Proxy to use (only http), another constructor otherwise.
     */
    public Instagram(@NotNull String username, @NotNull String password,InetSocketAddress proxy) throws IGLoginException {
        try {
            IGClient.Builder builder = IGClient.builder();
            builder.username(username).password(password);
            if (proxy != null)
                builder.client(new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP,proxy)).build());
            client = builder.login();
            utils = new Utils(client);
        } catch (com.github.instagram4j.instagram4j.exceptions.IGLoginException e) {
            if (e.getLoginResponse().getTwo_factor_info() != null) {
                throw new IGLoginException("This account requires 2-factor-authentication. Use second constructor for this type of login passing a callback as third argument.", Reasons.REQUIRE_2_FACTOR_AUTHENTICATION);
            } else if (e.getMessage().contains("few minutes"))
                throw new IGLoginException("Please wait for few minutes before trying again. Instagram blocks requests that are coming from same IP", Reasons.LOGIN_TOO_FREQUENTLY);
            else if (e.getMessage().contains("password"))
                throw new IGLoginException("Username or password is incorrect",Reasons.INVALID_CREDENTIALS);
            else if (e.getMessage().contains("challenge"))
                throw new IGLoginException("You account is temporary suspended by instagram. Open https://i.instagram.com/challenge to verify your account. Use loadInstance() method instead of using constructor every time to avoid this type of problem",Reasons.CHALLENGE_REQUIRED);
            else if (e.getMessage().contains("SocketException"))
                throw new IGLoginException("There is a problem with your proxy. Try different proxy or else don't use it",Reasons.PROXY_ERROR);
            else
                throw new IGLoginException("An unknown error occured. This must not happen in most of the case. If this problem persist, then please create a issue on github regarding this exception",Reasons.LOGIN_ERROR_UNKNOWN);
        }
    }


    /**
     * Loges in to your instagram account with the provided username and password and then authenticate using two-factor-authentication.
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     * @param callback The callback that will be invoked when verification code is sent. This will wait until you call {@link Instagram#verifyCode()}. You have to return the verification code.
     */
    public static Instagram login2factor(@NotNull String username, @NotNull String password, @NotNull Callable<String> callback) {
        try {
            client = IGClient.builder()
                    .username(username)
                    .password(password)
                    .onTwoFactor(new IGClient.Builder.LoginHandler() {
                        @Override
                        public LoginResponse accept(IGClient client, LoginResponse t) {
                            return IGChallengeUtils.resolveTwoFactor(client, t, new Callable<String>() {
                                @Override
                                public String call() throws Exception {
                                    client.wait(5*1000*60);
                                    return callback.call();
                                }
                            });
                        }
                    }).login();
            return new Instagram(client);
        } catch (com.github.instagram4j.instagram4j.exceptions.IGLoginException e) {
            if (e.getMessage().contains("few minutes"))
                throw new RuntimeException(new IGLoginException("Please wait for few minutes before trying again. Instagram blocks requests that are coming from same IP", Reasons.LOGIN_TOO_FREQUENTLY));
            else if (e.getMessage().contains("password"))
                throw new RuntimeException(new IGLoginException("Username or password is incorrect",Reasons.INVALID_CREDENTIALS));
            else if (e.getMessage().contains("challenge"))
                throw new RuntimeException(new IGLoginException("You account is temporary suspended by instagram. Open https://i.instagram.com/challenge to verify your account. Use loadInstance() method instead of using constructor every time to avoid this type of problem",Reasons.CHALLENGE_REQUIRED));
            else
                throw new RuntimeException(new IGLoginException("An unknown error occured. This must not happen in most of the case. If this problem persist, then please create a issue on github regarding this exception",Reasons.LOGIN_ERROR_UNKNOWN));
        }
    }


    public static void verifyCode(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.notifyAll();
            }
        }).start();
    }


    /**
     * Serialize instagram object to a file. This will cache the current instagram object and writes it to a file.
     * Using this, you can avoid logging in every time the program starts.
     * @param directory The directory on the filesystem where the cache should be saved.
     * @throws InstagramException - if the provided file is not a directory or is not writable.
     */
    private static void saveInstance(File directory) throws InstagramException {
        try {
            if (directory.isDirectory())
                client.serialize(new File(directory,"object"),new File(directory,"cookie"));
            else
                throw new InstagramException("IOException : File is not a directory",Reasons.SERIALIZATION_ERROR);
        } catch (IOException e) {
            throw new InstagramException(e.getLocalizedMessage(),Reasons.SERIALIZATION_ERROR);
        }
    }

    private static Instagram loadInstance(File directory){
        try {
            if (directory.isDirectory())
                return new Instagram(IGClient.deserialize(new File(directory,"object"),new File(directory,"cookie")));
            else
                throw new InstagramException("IOException : File is not a directory",Reasons.DESERIALIZATION_ERROR);
        } catch (ClassNotFoundException | IOException e) {
            throw new InstagramException(e.getLocalizedMessage(),Reasons.DESERIALIZATION_ERROR);
        }
    }


    /**
     * Sends a direct message to a user
     * @param username The username of the user.
     * @param message The text to send.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> directMessage(@NotNull String username, @NotNull String message) {
        try {
            User user = client.actions().users().findByUsername(username).get().getUser();
            DirectThreadsBroadcastRequest.BroadcastTextPayload payload = new DirectThreadsBroadcastRequest.BroadcastTextPayload(message, user.getPk());
            return utils.request(new DirectThreadsBroadcastRequest(payload));
        } catch (ExecutionException | InterruptedException | CompletionException e) {
            return new Task<>(e);
        }
    }


    /**
     * Sends a photo to the user.
     * @param username The username of the user.
     * @param photo The photo to send.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> directMessage(@NotNull String username, @NotNull File photo) {
        if (photo.getName().endsWith(".mp4"))
            throw new InstagramException("Uploading or sending video is not currently supported",Reasons.UNSUPPORTED_FILE_FORMAT);
        try {
            User user = client.actions().users().findByUsername(username).get().getUser();
            DirectThreadsBroadcastRequest.BroadcastPayload payload = new DirectThreadsBroadcastRequest.BroadcastConfigurePhotoPayload(photo.getName(), user.getPk());
            return utils.request(new DirectThreadsBroadcastRequest(payload));
        } catch (ExecutionException | InterruptedException | CompletionException e) {
            return new Task<>(e);
        }
    }

    /**
     * Follows the user.
     * @param username Username of the user to follow.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> follow(@NotNull String username) {
        return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.CREATE);
    }


    /**
     * Unfollows the user.
     * @param username Username of the user to unfollow.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> unfollow(@NotNull String username) {
        return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.DESTROY);
    }


    /**
     * Accept the pending follow request, if had any.
     * @param username Username of the user who had requested.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> accept(@NotNull String username) {
        return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.APPROVE);
    }


    /**
     * Ignores the pending follow request, if had any.
     * @param username Username of the user who had requested.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> ignore(@NotNull String username) {
        return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.IGNORE);
    }


    /**
     * Remove the user from your followers.
     * @param username Username of the user to remove.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> removeFollower(@NotNull String username) {
        return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.REMOVE_FOLLOWER);
    }


    /**
     * Post a new picture.
     * @param photo The photo to post
     * @param caption Caption for the post
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> postPhoto(@NotNull File photo, String caption) {
        if (photo.getName().endsWith(".mp4"))
            throw new InstagramException("Uploading or sending video is not currently supported",Reasons.UNSUPPORTED_FILE_FORMAT);

        Task<String> task = new Task<>();
        CompletableFuture<MediaResponse.MediaConfigureTimelineResponse> response = client.actions().timeline().uploadPhoto(photo, caption == null ? "Uploaded using EasyInsta library" : caption);
        try {
            response.thenAccept(new Consumer<MediaResponse.MediaConfigureTimelineResponse>() {
                @Override
                public void accept(MediaResponse.MediaConfigureTimelineResponse response) {
                    task.value = response.getMessage();
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


    /**
     * Currently not working. (Unimplemented)
     * @param pic The photo to replace
     */
    private void setDp(@NotNull File pic) {
        throw new UnsupportedOperationException("Don't try to be over smart");
    }


    /**
     * Sets the bio of your profile
     * @param bio The bio to set.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> setBio(@NotNull String bio) {
        if (bio.length() > 150)
            throw new InstagramException("Bio must be of less then 150 characters", Reasons.BIO_LENGTH_EXCEEDED);

        Task<String> task = new Task<>();
        CompletableFuture<IGResponse> response = client.actions().account().setBio(bio);
        try{
            response.thenAccept(new Consumer<IGResponse>() {
                @Override
                public void accept(IGResponse response) {
                    task.value = response.getMessage();
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    task.exception = throwable;
                    return null;
                }
            }).join();
            return task;
        } catch (CancellationException e){
            return new Task<>(e);
        }
    }


    /**
     * Adds a photo to your story.
     * @param photo The photo to add as story.
     * @return A {@link Task} holding response of the request.
     */
    public Task<String> addStory(@NotNull File photo) {
        if (photo.getName().endsWith(".mp4"))
            throw new InstagramException("Uploading or sending video is not currently supported",Reasons.UNSUPPORTED_FILE_FORMAT);

        Task<String> task = new Task<>();
        CompletableFuture<MediaResponse.MediaConfigureToStoryResponse> response = client.actions().story().uploadPhoto(photo);
        try {
            response.thenAccept(new Consumer<MediaResponse.MediaConfigureToStoryResponse>() {
                @Override
                public void accept(MediaResponse.MediaConfigureToStoryResponse response) {
                    task.value = response.getMessage();
                }
            }).exceptionally(new Function<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) {
                    task.exception = throwable;
                    return null;
                }
            }).join();
            return task;
        } catch (CancellationException e){
            return new Task<>(e);
        }
    }


    /**
     * Scraps followers of the account associated with the username. Empty list if none found but Never null.
     * @param username The username of the account from which you want to scrap.
     * @return A {@link Task} holding list of the followers.
     */
    public @NotNull Task<List<String>> getFollowers(@NotNull String username) {
        return utils.getFeeds(client.actions().users().findByUsername(username),true);
    }


    /**
     * Scraps followings of the account associated with the provided username. Empty list if none found but Never null.
     * @param username The username of the account from which you want to scrap.
     * @return A {@link Task} holding list of the followings.
     */
    public @NotNull Task<List<String>> getFollowings(@NotNull String username) {
        return utils.getFeeds(client.actions().users().findByUsername(username),false);
    }


    /**
     * Gets the pending follow requests. Empty list if none found but Never null.
     * @return A {@link Task} holding usernames of the requests.
     */
    public @NotNull Task<List<String>> getFollowRequests() {
        List<String> usernames = new ArrayList<>();
        Task<List<String>> task = new Task<>();
        CompletableFuture<FeedUsersResponse> response = client.sendRequest(new FriendshipsPendingRequest());
        response.thenAccept(new Consumer<FeedUsersResponse>() {
            @Override
            public void accept(FeedUsersResponse feedUsersResponse) {
                for (Profile user : feedUsersResponse.getUsers()) {
                    usernames.add(user.getUsername());
                }
                task.value = usernames;
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


    /**
     * Gets the bio of the account associated with the username.
     * @param username The username of the account.
     * @return A {@link Task} holding bio of the user.
     */
    public Task<String> getBio(@NotNull String username) {
        Object object =  utils.getProfileMetadata(client.actions().users().findByUsername(username),"bio");
        Task<String> task = new Task<>();

        if (object instanceof Throwable)
            task.exception = (Throwable) object;
        else
            task.value = (String) object;

        return task;
    }


    /**
     * Gets the profile photo link of the account associated with the username provided.
     * @param username The username of the account.
     * @return A {@link Task} holding profile picture of the user.
     */
    public Task<String> getProfilePicUrl(@NotNull String username) {
        Object object =  utils.getProfileMetadata(client.actions().users().findByUsername(username),"dp");
        Task<String> task = new Task<>();

        if (object instanceof Throwable)
            task.exception = (Throwable) object;
        else
            task.value = (String) object;

        return task;
    }


    /**
     * Gets the follower count of the account associated with the username.
     * @param username The username of the account.
     * @return A {@link Task} holding number of followers of the user.
     */
    public Task<Integer> getFollowersCount(@NotNull String username) {
        Object object =  utils.getProfileMetadata(client.actions().users().findByUsername(username),"followers");
        Task<Integer> task = new Task<>();

        if (object instanceof Throwable)
            task.exception = (Throwable) object;
        else
            task.value = (int) object;

        return task;
    }


    /**
     * Gets the followings count of the account associated with the username provided.
     * @param username The username of the account.
     * @return A {@link Task} holding number of followings of the user.
     */
    public Task<Integer> getFollowingsCount(@NotNull String username) {
        Object object =  utils.getProfileMetadata(client.actions().users().findByUsername(username),"followings");
        Task<Integer> task = new Task<>();

        if (object instanceof Throwable)
            task.exception = (Throwable) object;
        else
            task.value = (int) object;

        return task;
    }


    /**
     * Gets the post count of the account associated with the username provided.
     * @param username The username of the account.

     * @return A {@link Task} holding number of posts of the user.
     */
    public Task<Integer> getPostCount(@NotNull String username) {
        Object object =  utils.getProfileMetadata(client.actions().users().findByUsername(username),"post");
        Task<Integer> task = new Task<>();

        if (object instanceof Throwable)
            task.exception = (Throwable) object;
        else
            task.value = (int) object;

        return task;
    }
}
