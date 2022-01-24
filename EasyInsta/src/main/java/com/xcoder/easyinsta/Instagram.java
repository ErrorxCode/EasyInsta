package com.xcoder.easyinsta;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.direct.IGThread;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadItem;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadTextItem;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.direct.DirectCreateGroupThreadRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectInboxRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsBroadcastRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsDeleteItemRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsMarkItemSeenRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsPendingRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.accounts.LoginResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import com.github.instagram4j.instagram4j.responses.media.MediaResponse;
import com.github.instagram4j.instagram4j.utils.IGChallengeUtils;
import com.github.instagram4j.instagram4j.utils.IGUtils;
import com.xcoder.easyinsta.exceptions.IGLoginException;
import com.xcoder.easyinsta.exceptions.InstagramException;
import com.xcoder.easyinsta.exceptions.Reasons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is an easy implementation of instagram private api in java. This class include various methods to perform instagram operation like
 * direct messages,adding stories,posting photos,scrapping profile data like posts,followers,followings etc. All the methods runs synchronously.
 *
 * @author Rahil khan
 * @version 1.5
 */
public class Instagram {
    private static IGClient client;
    private static Utils utils;
    private static final int MESSAGE_ALL = 0;
    private static final int FREQUENCY_ALL = 0;
    private static final int FREQUENCY_FIRST = 1;
    private static final int FREQUENCY_SECOND = 2;
    private static final int FREQUENCY_THIRD = 3;

    private Instagram(IGClient client) {
        utils = new Utils(client);
        Instagram.client = client;
    }


    /**
     * Loges in to your instagram account with the provided username and password. If your account has two-factor authentication,
     * then use static {@code loginOrCache()} method which takes a {@link Callable<String>} as an argument.
     *
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     * @param proxy    Proxy to use (only http), null otherwise.
     */
    public static Instagram login(@NotNull String username, @NotNull String password, @Nullable Proxy proxy) throws IGLoginException {
        try {
            IGClient.Builder builder = IGClient.builder();
            builder.username(username).password(password);
            if (proxy != null)
                builder.client(IGUtils.defaultHttpClientBuilder().proxy(proxy).build());
            utils = new Utils(client);
            return new Instagram(builder.login());
        } catch (com.github.instagram4j.instagram4j.exceptions.IGLoginException e) {
            if (e.getLoginResponse().getTwo_factor_info() != null) {
                throw new IGLoginException("This account requires 2-factor-authentication. Use second constructor for this type of login passing a callback as third argument.", Reasons.REQUIRE_2_FACTOR_AUTHENTICATION);
            } else if (e.getMessage().contains("few minutes"))
                throw new IGLoginException("Please wait for few minutes before trying again. Instagram blocks requests that are coming from same IP", Reasons.LOGIN_TOO_FREQUENTLY);
            else if (e.getMessage().contains("password"))
                throw new IGLoginException("Username or password is incorrect", Reasons.INVALID_CREDENTIALS);
            else if (e.getMessage().contains("challenge"))
                throw new IGLoginException("You account is temporary suspended by instagram. Open https://i.instagram.com/challenge to verify your account. Use loadInstance() method instead of using constructor every time to avoid this type of problem", Reasons.CHALLENGE_REQUIRED);
            else if (e.getMessage().contains("SocketException"))
                throw new IGLoginException("There is a problem with your proxy. Try different proxy or else don't use it", Reasons.PROXY_ERROR);
            else
                throw new IGLoginException("An unknown error occured. This must not happen in most of the case. If this problem persist, then please create a issue on github regarding this exception", Reasons.LOGIN_ERROR_UNKNOWN);
        }
    }


    /**
     * Notice : Unimplemented !
     * Loges in to your instagram account with the provided username and password and then authenticate using two-factor-authentication.
     *
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     * @param callback The callback that will be invoked when verification code is sent. This will wait until you call. You have to return the verification code.
     */
    private static Instagram login2factor(@NotNull String username, @NotNull String password, @NotNull Callable<String> callback) throws IGLoginException {
        try {
            return new Instagram(IGClient.builder()
                    .username(username)
                    .password(password)
                    .onTwoFactor(new IGClient.Builder.LoginHandler() {
                        @Override
                        public LoginResponse accept(IGClient client, LoginResponse t) {
                            return IGChallengeUtils.resolveTwoFactor(client, t, new Callable<String>() {
                                @Override
                                public String call() throws Exception {
                                   // have to impliment
                                    return null;
                                }
                            });
                        }
                    }).login());
        } catch (com.github.instagram4j.instagram4j.exceptions.IGLoginException e) {
            e.printStackTrace();
            if (e.getMessage().contains("few minutes"))
                throw new IGLoginException("Please wait for few minutes before trying again. Instagram blocks requests that are coming from same IP", Reasons.LOGIN_TOO_FREQUENTLY);
            else if (e.getMessage().contains("password"))
                throw new IGLoginException("Username or password is incorrect", Reasons.INVALID_CREDENTIALS);
            else if (e.getMessage().contains("challenge"))
                throw new IGLoginException("You account is temporary suspended by instagram. Open https://i.instagram.com/challenge to verify your account. Use loadInstance() method instead of using constructor every time to avoid this type of problem", Reasons.CHALLENGE_REQUIRED);
            else
                throw new IGLoginException("An unknown error occured. This must not happen in most of the case. If this problem persist, then please create a issue on github regarding this exception", Reasons.LOGIN_ERROR_UNKNOWN);
        }
    }


    /**
     * Returns pre-logged in instagram instance from the cache. If no cache exist, It first perform login & then cache it so it can be used in future.
     *
     * @param dir      The cache directory of the application. See {@link android.content.Context#getCacheDir()}
     * @param username Username of the account
     * @param password Password of the account
     * @return {@link Instagram} instance.
     * @throws IGLoginException If the dir is not a directory or if an IO error occured while serializing/deserializing the class.
     */
    public static Instagram loginOrCache(@NotNull File dir, @NotNull String username, @NotNull String password) throws IGLoginException, IOException {
        try {
            File client = new File(dir, "ClientObject.cache");
            File cookie = new File(dir, "LoginSession.cache");
            if (client.exists() && cookie.exists())
                return new Instagram(IGClient.deserialize(client, cookie));
            else {
                Instagram instagram = login(username, password, null);
                Instagram.client.serialize(client, cookie);
                return instagram;
            }
        } catch (ClassNotFoundException e) {
            throw new InstagramException(e.getLocalizedMessage(), Reasons.CACHING_ERROR);
        }
    }

    /**
     * Caches the current instagram session, Overwrite if already exist. There is no guarantee that it will always cache. It may fail sometimes (0.1% chance)
     *
     * @param dir The cache directory of the application. See {@link android.content.Context#getCacheDir()}
     */
    public void cache(@NotNull File dir) {
        File client = new File(dir, "ClientObject.cache");
        File cookie = new File(dir, "LoginSession.cache");
        try {
            Instagram.client.serialize(client, cookie);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears the instagram cache if exist.
     * @param dir The directory where cache is stored.
     */
    public void clearCache(@NotNull File dir){
        File client = new File(dir, "ClientObject.cache");
        File cookie = new File(dir, "LoginSession.cache");
        client.delete();
        cookie.delete();
    }

    /**
     * Gets the profile of the user. This profile include all the methods related to it.
     * @param username Username of the user for getting her/his profile
     * @return {@link Profile} of the user.
     */
    public Profile getProfile(@NotNull String username){
        return new Profile(username);
    }

    /**
     * Gets your feeds. This feed include all the methods related to your feed.
     * @return {@link Feed} your home feed
     */
    public Feed getFeed(){
        return new Feed();
    }

    /**
     * Gets yours direct inbox of This direct include all the methods related to direct messaging.
     * @param username Username of the user in your inbox, whole you want to send message or read message.
     * @return {@link Direct} Your direct inbox.
     */
    public Direct getDirect(@NotNull String username){
        return new Direct(username);
    }

    /**
     * Gets wrapper of all the common action of instagram.
     * @return {@link Actions}
     */
    public Actions actions(){
        return new Actions();
    }




    public interface OnProgressListener {
        void onProgress(int percentage);
    }

    /**
     * Interface for handling callbacks of spam messages.
     */
    public interface OnMessageActionCallback {
        void onSuccess(String message);
        void onFailed(Exception exception);
        void onProgress(int percentage);
    }





    /**
     * This class includes all the methods related to chat & messaging.
     */
    public static final class Direct {
        private final String username;


        private Direct(String username){
            this.username = username;
        }


        /**
         * Sends a direct message to a user
         * @param message  The text to send.
         * @return A {@link Task} indication success or failure of the method
         */
        public Task<Void> directMessage(@NotNull String message) {
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
         * @param photo    The photo to send.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> directMessage(@NotNull File photo) {
            if (!photo.getName().endsWith(".jpg") || !photo.getName().endsWith(".png") || !photo.getName().endsWith("jpeg"))
                throw new InstagramException("Unsupported file format. Only photos with jpg/png/jpeg extensions are allowed", Reasons.UNSUPPORTED_FILE_FORMAT);
            try {
                User user = client.actions().users().findByUsername(username).get().getUser();
                DirectThreadsBroadcastRequest.BroadcastPayload payload = new DirectThreadsBroadcastRequest.BroadcastConfigurePhotoPayload(photo.getName(), user.getPk());
                return utils.request(new DirectThreadsBroadcastRequest(payload));
            } catch (ExecutionException | InterruptedException | CompletionException e) {
                return new Task<>(e);
            }
        }

        /**
         * Marks message as seen that was sent by the username.
         * @return A {@link Task} indication success or failure of the request
         */
        @NotNull
        public Task<Void> seeMessage() {
            try {
                User user = client.actions().users().findByUsername(username).get().getUser();
                return utils.request(new DirectThreadsMarkItemSeenRequest("340282366841710300949128131112046895852", "30300890545456593390597190992265216"));
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return new Task<>(e);
            }
        }


        /**
         * Retrieve messages from a particular chat from last till number specified.
         * @param howMany The number of messages to get. (including recipient chat). 0 for all
         * @param listener A listener for tracking progress. only works when count > 20
         * @return A {@link Task} holding list of messages.
         * @throws IllegalArgumentException if howMany is negative.
         */
        private Task<List<String>> getChatMessages(int howMany,@Nullable OnProgressListener listener){
            Task<List<String>> task = new Task<>();
            try {
                List<ThreadItem> items = utils.getThreadItem(username, howMany,false,listener);
                List<String> messages = new ArrayList<>();
                for (ThreadItem item : items){
                    if (item instanceof ThreadTextItem)
                        messages.add(utils.isMessageSent(item) ? "You : " + ((ThreadTextItem) item).getText() : username + " : " + ((ThreadTextItem) item).getText());
                    else
                        messages.add(utils.isMessageSent(item) ? "You : [\uD83C\uDFA5] ---[ Shared a post ] ---" : username + " : [\uD83C\uDFA5] ---[ Shared a post ] ---");
                }
                Collections.reverse(messages);
                task.value = messages;
            } catch (InterruptedException | ExecutionException e) {
                task.exception = e;
            }
            return task;
        }

        /**
         * Retrieve messages from a particular chat from a particular message till last.
         * @param fromMessage The message from where to start fetching messages. This is case-sensitive
         * @param frequency If 'fromMessage' is occured multiple time, then pass the no. from where to consider.
         *                  For example, To retrieve messages from 'hey' and if there are 3 more same messages (exact equal) like 'hay'
         *                  then pass 1 to get from last 'hey' or 2 or 3 from the top second or first 'hey'.
         * @return A {@link Task} holding list of messages.
         * @throws IllegalArgumentException if frequency is negative.
         */
        @NotNull
        public Task<List<String>> getChatMessagesFrom(@NotNull String fromMessage,int frequency){
            Task<List<String>> task = new Task<>();
            try {
                List<String> messages = new ArrayList<>();
                List<ThreadItem> items = utils.getThreadItem(username, fromMessage,frequency,false,null);
                for (ThreadItem item : items){
                    if (item instanceof ThreadTextItem)
                        messages.add(utils.isMessageSent(item) ? "You : " + ((ThreadTextItem) item).getText() : username + " : " + ((ThreadTextItem) item).getText());
                    else
                        messages.add(utils.isMessageSent(item) ? "You : [\uD83C\uDFA5] ---[ Shared a post ] ---" : username + " : [\uD83C\uDFA5] ---[ Shared a post ] ---");
                }
                Collections.reverse(messages);
                task.value = messages;
            } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
                task.exception = e;
            }
            return task;
        }


        /**
         * Broadcast a text message in a direct chat group.
         * @param message The message to broadcast.
         * @param adminUsername The username of any one admin of the group. If the same user is admin of 2 or more group then the top first
         *                      group will be selected.
         * @return A {@link Task} indication success or failure of the request
         */
        @NotNull
        public Task<Void> groupMessage(@NotNull String message,@NotNull String adminUsername){
            try {
                List<IGThread> threads = client.sendRequest(new DirectInboxRequest()).get().getInbox().getThreads();
                Long pk = client.actions().users().findByUsername(adminUsername).get().getUser().getPk();
                for (IGThread thread : threads){
                    if (thread.is_group() && thread.getAdmin_user_ids().contains(String.valueOf(pk))) {
                        DirectThreadsBroadcastRequest request = new DirectThreadsBroadcastRequest(new DirectThreadsBroadcastRequest.BroadcastTextPayload(message,thread.getThread_id()));
                        return utils.request(request);
                    }
                }
                return new Task<>(new InstagramException(adminUsername + " is not admin of any group in your inbox",Reasons.NO_SUCH_ADMIN));
            } catch (ExecutionException | InterruptedException e) {
                return new Task<>(e);
            }
        }


        /**
         * Unimplemented !
         * @param groupName The title of the group
         * @param usernames The usernames of the members to add in the group.
         * @return A {@link Task} indication success or failure of the request
         */
        @NotNull
        private Task<Void> createGroup(@NotNull String groupName,@NotNull String... usernames){
            String[] pks = new String[usernames.length];
            try {
                for (int i = 0; i < pks.length; i++) {
                    pks[i] = String.valueOf(client.actions().users().findByUsername(usernames[i]).get().getUser().getPk());
                }
                DirectCreateGroupThreadRequest request = new DirectCreateGroupThreadRequest(groupName,pks);
                return utils.request(request);
            } catch (ExecutionException | InterruptedException e) {
                return new Task<>(new InstagramException("One of the username is not valid.",Reasons.INVALID_USERNAME));
            }
        }


        /**
         * Send mass messages to a user at a single time (also called spamming)
         * @param count The no. of message to sent
         * @param messages list of String containing messages to sent. a random message picked will be sent from this list.
         * @param callback Callback indication success and failure of each message.
         */
        public void spamDM(int count,@NotNull String[] messages,@Nullable OnMessageActionCallback callback){
            for (int i = 0; i < count; i++) {
                String message = messages[new Random().nextInt(messages.length)];
                directMessage(message).addOnCompleteListener(new Task.OnCompletionListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (callback != null){
                            if (task.isSuccessful())
                                callback.onSuccess(message);
                            else
                                callback.onFailed((Exception) task.getException());
                        }
                    }
                });
            }
        }


        /**
         * Send mass messages to a group at a single time (also called spamming)
         * @param count The no. of message to sent
         * @param adminUsername Username of admin of the group. If this user is also admin of another group, then the Top first group in your inbox will be considered.
         * @param messages list of String containing messages to sent. a random message picked will be sent from this list.
         * @param callback Callback indication success and failure of each message.
         */
        public void spamGroup(int count,@NotNull String adminUsername,@NotNull String[] messages,@Nullable OnMessageActionCallback callback){
            for (int i = 0; i < count; i++) {
                String message = messages[new Random().nextInt(messages.length)];
                groupMessage(message,adminUsername).addOnCompleteListener(new Task.OnCompletionListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (callback != null){
                            if (task.isSuccessful())
                                callback.onSuccess(message);
                            else
                                callback.onFailed((Exception) task.getException());
                        }
                    }
                });
            }
        }

        /**
         * Deletes the messages from the last till the particular number.
         * @param howMany The no. of messages (including recipient message) to delete. For example,
         *               if there are last 2 message from him/her and then 3 messages from you,
         *                so you have to pass 5 here to delete your 3 messages. Pass 0 to delete all.
         * @param callback The callback indication success or failure of each delete message request.
         *@throws IllegalArgumentException if howMany is negative.
         */
        public void deleteMessages(int howMany,@Nullable OnMessageActionCallback callback){
            try {
                for (ThreadItem item : utils.getThreadItem(username, howMany,true,null))
                    utils.request(new DirectThreadsDeleteItemRequest(utils.getThread(username).getThread_id(),item.getItem_id())).addOnCompleteListener(new Task.OnCompletionListener<Void>() {
                        @Override
                        public void onComplete(Task<Void> task) {
                            if (callback != null){
                                if (task.isSuccessful()){
                                    if (item.getItem_type().equals("text"))
                                        callback.onSuccess("\"" + ((ThreadTextItem) item).getText() + "\"");
                                    else
                                        callback.onSuccess("[!] A shared post");
                                } else {
                                    callback.onFailed((Exception) task.getException());
                                }
                            }
                        }
                    });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }



        public void deleteMessagesFrom(@NotNull String fromMessage,int frequency,@Nullable OnMessageActionCallback callback){
            try {
                for (ThreadItem item : utils.getThreadItem(username,fromMessage,frequency,true,null))
                    utils.request(new DirectThreadsDeleteItemRequest(utils.getThread(username).getThread_id(),item.getItem_id())).addOnCompleteListener(new Task.OnCompletionListener<Void>() {
                        @Override
                        public void onComplete(Task<Void> task) {
                            if (callback != null){
                                if (task.isSuccessful()){
                                    if (item.getItem_type().equals("text"))
                                        callback.onSuccess("\"" + ((ThreadTextItem) item).getText() + "\"");
                                    else
                                        callback.onSuccess("[!] A shared post");
                                } else {
                                    callback.onFailed((Exception) task.getException());
                                }
                            }
                        }
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    /**
     * This class includes all the methods related to profile of the user.
     */
    public static final class Profile {
        private final String username;

        private Profile(String username){
            this.username = username;
        }


        /**
         * Currently not working. (Unimplemented)
         *
         * @param pic The photo to replace
         */
        private void setDp(@NotNull File pic) {
            throw new UnsupportedOperationException("Don't try to be over smart");
        }


        /**
         * Sets the bio of your profile
         *
         * @param bio The bio to set.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> setBio(@NotNull String bio) {
            if (bio.length() > 150)
                throw new InstagramException("Bio must be of less then 150 characters", Reasons.BIO_LENGTH_EXCEEDED);

            Task<Void> task = new Task<>();
            try {
                client.actions().account().setBio(bio).exceptionally(new Function<Throwable, IGResponse>() {
                    @Override
                    public IGResponse apply(Throwable throwable) {
                        task.exception = throwable;
                        return null;
                    }
                }).join();
                return task;
            } catch (CancellationException e) {
                return new Task<>(e);
            }
        }


        /**
         * Scraps 100 followers of the account associated with the username. Empty list if none found but Never null.
         * @return A {@link Task} holding list of the followers.
         */
        public @NotNull Task<List<String>> getFollowers() {
            return utils.getFeeds(client.actions().users().findByUsername(username), true);
        }


        /**
         * Scraps 100 followings of the account associated with the provided username. Empty list if none found but Never null.
         * @return A {@link Task} holding list of the followings.
         */
        public @NotNull Task<List<String>> getFollowings() {
            return utils.getFeeds(client.actions().users().findByUsername(username), false);
        }


        /**
         * Gets the pending follow requests of yours.
         * @return A {@link Task} holding usernames of the requests.
         */
        public @NotNull Task<List<String>> getFollowRequests() {
            List<String> usernames = new ArrayList<>();
            Task<List<String>> task = new Task<>();
            CompletableFuture<FeedUsersResponse> response = client.sendRequest(new FriendshipsPendingRequest());
            response.thenAccept(new Consumer<FeedUsersResponse>() {
                @Override
                public void accept(FeedUsersResponse feedUsersResponse) {
                    for (com.github.instagram4j.instagram4j.models.user.Profile user : feedUsersResponse.getUsers()) {
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
         * Gets the bio of the account associated with the provided username.
         * @return A {@link Task} holding bio of the user.
         */
        public @NotNull Task<String> getBio() {
            Object object = utils.getProfileMetadata(client.actions().users().findByUsername(username), "bio");
            Task<String> task = new Task<>();

            if (object instanceof Throwable)
                task.exception = (Throwable) object;
            else
                task.value = (String) object;

            return task;
        }


        /**
         * Gets the profile photo link of the account associated with the username provided.
         * @return A {@link Task} holding url of profile picture of the user.
         */
        public @NotNull Task<String> getProfilePicUrl() {
            Object object = utils.getProfileMetadata(client.actions().users().findByUsername(username), "dp");
            Task<String> task = new Task<>();

            if (object instanceof Throwable)
                task.exception = (Throwable) object;
            else
                task.value = (String) object;

            return task;
        }


        /**
         * Gets the follower count of the account associated with the username.
         * @return A {@link Task} holding number of followers of the user.
         */
        @NotNull
        public Task<Integer> getFollowersCount() {
            Object object = utils.getProfileMetadata(client.actions().users().findByUsername(username), "followers");
            Task<Integer> task = new Task<>();

            if (object instanceof Throwable)
                task.exception = (Throwable) object;
            else
                task.value = (int) object;

            return task;
        }


        /**
         * Gets the followings count of the account associated with the username provided.
         * @return A {@link Task} holding number of followings of the user.
         */
        @NotNull
        public Task<Integer> getFollowingsCount() {
            Object object = utils.getProfileMetadata(client.actions().users().findByUsername(username), "followings");
            Task<Integer> task = new Task<>();

            if (object instanceof Throwable)
                task.exception = (Throwable) object;
            else
                task.value = (int) object;

            return task;
        }


        /**
         * Gets the post count of the account associated with the username provided.
         * @return A {@link Task} holding number of posts of the user.
         */
        @NotNull
        public Task<Integer> getPostCount() {
            Object object = utils.getProfileMetadata(client.actions().users().findByUsername(username), "post");
            Task<Integer> task = new Task<>();

            if (object instanceof Throwable)
                task.exception = (Throwable) object;
            else
                task.value = (int) object;

            return task;
        }
    }





    /**
     * This class includes all the methods related to your feed.
     */
    public static final class Feed {
        /**
         * Post a new picture.
         *
         * @param photo   The photo to post
         * @param caption Caption for the post
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> postPhoto(@NotNull File photo, String caption) {
            if (photo.getName().endsWith(".mp4"))
                throw new InstagramException("Uploading or sending video is not currently supported", Reasons.UNSUPPORTED_FILE_FORMAT);

            Task<Void> task = new Task<>();
            try {
                client.actions().timeline().uploadPhoto(photo, caption == null ? "Uploaded using EasyInsta library" : caption)
                        .exceptionally(new Function<Throwable, MediaResponse.MediaConfigureTimelineResponse>() {
                            @Override
                            public MediaResponse.MediaConfigureTimelineResponse apply(Throwable throwable) {
                                task.exception = throwable;
                                return null;
                            }
                        }).join();
                return task;
            } catch (CompletionException e) {
                return new Task<>(e);
            }
        }


        /**
         * Adds a photo to your story.
         *
         * @param photo The photo to add as story.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> addStory(@NotNull File photo) {
            if (photo.getName().endsWith(".mp4"))
                throw new InstagramException("Uploading or sending video is not currently supported", Reasons.UNSUPPORTED_FILE_FORMAT);

            Task<Void> task = new Task<>();
            try {
                client.actions().story().uploadPhoto(photo).exceptionally(new Function<Throwable, MediaResponse.MediaConfigureToStoryResponse>() {
                    @Override
                    public MediaResponse.MediaConfigureToStoryResponse apply(Throwable throwable) {
                        task.exception = throwable;
                        return null;
                    }
                }).join();
                return task;
            } catch (CancellationException e) {
                return new Task<>(e);
            }
        }

    }


    /**
     * This class includes all common action of your instagram, like following someone,accepting follow request,removing followers etc.
     */
    public static final class Actions {

        private Actions(){};

        /**
         * Follows the user.
         * @param username Username of the user to follow.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> follow(@NotNull String username) {
            return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.CREATE);
        }


        /**
         * Unfollows the user.
         *
         * @param username Username of the user to unfollow.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> unfollow(@NotNull String username) {
            return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.DESTROY);
        }


        /**
         * Accept the pending follow request, if had any.
         *
         * @param username Username of the user who had requested.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> accept(@NotNull String username) {
            return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.APPROVE);
        }


        /**
         * Ignores the pending follow request, if had any.
         *
         * @param username Username of the user who had requested.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> ignore(@NotNull String username) {
            return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.IGNORE);
        }


        /**
         * Remove the user from your followers.
         *
         * @param username Username of the user to remove.
         * @return A {@link Task} indication success or failure of the request
         */
        public Task<Void> removeFollower(@NotNull String username) {
            return utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.REMOVE_FOLLOWER);
        }
    }

    public static void main(String[] args) throws IGLoginException, IOException {
        Instagram instagram = Instagram.loginOrCache(new File("C:\\Users\\Asus\\Desktop"),"","");
    }
}