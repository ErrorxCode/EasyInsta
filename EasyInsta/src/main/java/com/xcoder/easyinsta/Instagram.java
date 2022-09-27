package com.xcoder.easyinsta;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.direct.IGThread;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadItem;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadTextItem;
import com.github.instagram4j.instagram4j.requests.direct.DirectCreateGroupThreadRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectInboxRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsBroadcastRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsDeleteItemRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsMarkItemSeenRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsPendingRequest;
import com.github.instagram4j.instagram4j.responses.accounts.LoginResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import com.github.instagram4j.instagram4j.utils.IGChallengeUtils;
import com.github.instagram4j.instagram4j.utils.IGUtils;
import com.github.instagram4j.realtime.IGRealtimeClient;
import com.github.instagram4j.realtime.mqtt.packet.PublishPacket;
import com.github.instagram4j.realtime.utils.PacketUtil;
import com.github.instagram4j.realtime.utils.ZipUtil;
import com.xcoder.easyinsta.exceptions.IGLoginException;
import com.xcoder.easyinsta.exceptions.InstagramException;
import com.xcoder.easyinsta.exceptions.Reasons;
import com.xcoder.easyinsta.interfaces.OnMessageActionCallback;
import com.xcoder.easyinsta.interfaces.OnNotificationListener;
import com.xcoder.easyinsta.interfaces.OnProgressListener;
import com.xcoder.tasks.AsyncTask;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is an easy implementation of instagram private api in java. This class include various methods to perform instagram operation like
 * direct messages,adding stories,posting photos,scrapping profile data like posts,followers,followings etc. All the methods runs asynchronously.
 *
 * @author Rahil khan
 * @version 1.5
 */
public class Instagram {
    private final IGClient client;
    private final Utils utils;
    public String username;
    public static final int MESSAGE_ALL = 0;
    public static final int FREQUENCY_ALL = 0;
    public static final int FREQUENCY_FIRST = 1;
    public static final int FREQUENCY_SECOND = 2;
    public static final int FREQUENCY_THIRD = 3;

    private Instagram(IGClient client) {
        this.client = client;
        utils = new Utils(client);
        username = client.getSelfProfile().getUsername();
    }


    /**
     * Loges in to your instagram account with the provided username and password. If your account has two-factor authentication,
     * then use static {@code loginOrCache()} method which takes a {@link Callable<String>} as an argument.
     *
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     */
    public static Instagram login(@NotNull String username, @NotNull String password) throws IGLoginException {
        try {
            IGClient.Builder builder = IGClient.builder();
            builder.username(username).password(password);
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
                throw new IGLoginException("An unknown error occurred. This must not happen in most of the case. If this problem persist, " +
                        "then please create a issue on github regarding this exception.\nStack trace :-\n" + e.getLocalizedMessage(), Reasons.LOGIN_ERROR_UNKNOWN);
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
                    .onTwoFactor((client, t) -> IGChallengeUtils.resolveTwoFactor(client, t, callback)).login());
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
     * Returns pre-logged in instagram instance from the cache. If no cache exist, It first perform login and then cache it so it can be used in future.
     *
     * @param dir      The cache directory of the application.
     * @param username Username of the account
     * @param password Password of the account
     * @return {@link Instagram} instance.
     * @throws IGLoginException If the dir is not a directory or if an IO error occured while serializing/deserializing the class.
     */
    public static Instagram loginOrCache(@NotNull File dir, @NotNull String username, @NotNull String password) throws IGLoginException {
        try {
            File client = new File(dir, "IGClient.ser");
            File cookie = new File(dir, "IGLoginSession.ser");
            if (client.exists() && cookie.exists())
                return new Instagram(IGClient.deserialize(client, cookie));
            else {
                Instagram instagram = login(username, password);
                instagram.client.serialize(client, cookie);
                return instagram;
            }
        } catch (ClassNotFoundException | IOException e) {
            throw new InstagramException(e.getLocalizedMessage(), Reasons.CACHING_ERROR);
        }
    }

     /**
     * Returns pre-logged in instagram instance from the cache. If no cache exist, It first perform login and then cache it so it can be used in future.
     *
     * @param dir      The cache directory of the application.
     * @param username Username of the account
     * @param password Password of the account
     * @param callback The callback that will be invoked when verification code is sent. This will wait until you call. You have to return the verification code.
     * @return {@link Instagram} instance.
     * @throws IGLoginException If the dir is not a directory or if an IO error occured while serializing/deserializing the class.
     */
    public static Instagram loginOrCache2factor(@NotNull File dir, @NotNull String username, @NotNull String password, @NotNull Callable<String> callback) throws IGLoginException {
        try {
            File client = new File(dir, "IGClient.ser");
            File cookie = new File(dir, "IGLoginSession.ser");
            if (client.exists() && cookie.exists())
                return new Instagram(IGClient.deserialize(client, cookie));
            else {
                Instagram instagram = login2factor(username, password, callback);
                instagram.client.serialize(client, cookie);
                return instagram;
            }
        } catch (ClassNotFoundException | IOException e) {
            throw new InstagramException(e.getLocalizedMessage(), Reasons.CACHING_ERROR);
        }
    }

    /**
     * Caches the current instagram session, Overwrite if already exist. There is no guarantee that it will always cache. It may fail sometimes (0.1% chance)
     *
     * @return true if the cache was successful. false otherwise.
     * @param dir The cache directory of the application.
     */
    public boolean cache(@NotNull File dir) {
        File client = new File(dir, "IGClient.ser");
        File cookie = new File(dir, "IGLoginSession.ser");
        try {
            this.client.serialize(client, cookie);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns weather the current session is been cached or not. If a session is cached then
     * it is considered logged in, else not.
     *
     * @param dir Directory where cache is saved
     * @return true if logged in,false otherwise
     */
    public static boolean isLogin(File dir) {
        return new File(dir, "LoginSession.cache").exists();
    }

    /**
     * Clears the instagram cache if exist.
     *
     * @param dir The directory where cache is stored.
     */
    public void clearCache(@NotNull File dir) {
        File client = new File(dir, "IGClient.ser");
        File cookie = new File(dir, "IGLoginSession.ser");
        client.delete();
        cookie.delete();
    }

    /**
     * Gets the profile of the user. This profile include all the methods related to it.
     *
     * @return {@link Profile} of the user.
     */
    public Profile profile() {
        return new Profile();
    }

    /**
     * Gets your feeds. This feed include all the methods related to your feed.
     *
     * @return {@link Feed} your home feed
     */
    public Feed feed() {
        return new Feed();
    }

    /**
     * Gets yours direct inbox of This direct include all the methods related to direct messaging.
     *
     * @return {@link Direct} Your direct inbox.
     */
    public Direct direct() {
        return new Direct();
    }

    /**
     * Gets wrapper of all the common action of instagram.
     *
     * @return {@link Actions}
     */
    public Actions actions() {
        return new Actions();
    }



    /**
     * This class includes all the methods related to chat and messaging.
     */
    public final class Direct {

        private Thread thread;

        /**
         * Sends a direct message to a user
         *
         * @param message The text to send.
         * @param username The user to send the message to.
         * @return A {@link AsyncTask} indication success or failure of the method
         */
        public AsyncTask<Void> directMessage(@NotNull String username,@NotNull String message) {
            return AsyncTask.callAsync(() -> {
                long user = client.actions().users().findByUsername(username).get().getUser().getPk();
                DirectThreadsBroadcastRequest.BroadcastTextPayload payload = new DirectThreadsBroadcastRequest.BroadcastTextPayload(message, user);
                utils.request(new DirectThreadsBroadcastRequest(payload));
                return null;
            });
        }


        /**
         * Sends a photo to the user.
         *
         * @param photo The photo to send.
         * @param username The user to send the message to.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> directMessage(@NotNull String username,@NotNull File photo) {
            if (!photo.getName().endsWith(".jpg") || !photo.getName().endsWith(".png") || !photo.getName().endsWith("jpeg"))
                throw new InstagramException("Unsupported file format. Only photos with jpg/png/jpeg extensions are allowed", Reasons.UNSUPPORTED_FILE_FORMAT);

            return AsyncTask.callAsync(() -> {
                long user = client.actions().users().findByUsername(username).get().getUser().getPk();
                DirectThreadsBroadcastRequest.BroadcastPayload payload = new DirectThreadsBroadcastRequest.BroadcastConfigurePhotoPayload(photo.getName(), user);
                utils.request(new DirectThreadsBroadcastRequest(payload));
                return null;
            });
        }

        /**
         * Marks message as seen that was sent by the username.
         * @param username The user to see the message of.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        @NotNull
        public AsyncTask<Void> seeMessage(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                var thread = utils.getThread(username);
                utils.request(new DirectThreadsMarkItemSeenRequest(thread.getThread_id(), thread.getItems().get(0).getItem_id()));
                return null;
            });
        }


        /**
         * Retrieve messages from a particular chat from last till number specified.
         *
         * @param howMany  The number of messages to get. (including recipient chat). 0 for all
         * @param username The user to get the message from.
         * @param listener A listener for tracking progress. only works when count > 20
         * @return A {@link AsyncTask} holding list of messages.
         * @throws IllegalArgumentException if howMany is negative.
         */
        public AsyncTask<List<String>> getChatMessages(@NotNull String username,int howMany, @Nullable OnProgressListener listener) {
            return AsyncTask.callAsync(() -> {
                List<ThreadItem> items = utils.getThreadItem(username, howMany, false, listener);
                List<String> messages = new ArrayList<>();
                for (ThreadItem item : items) {
                    if (item instanceof ThreadTextItem)
                        messages.add(utils.isMessageSent(item) ? "You : " + ((ThreadTextItem) item).getText().replace("\n", "") : username + " : " + ((ThreadTextItem) item).getText().replace("\n", ""));
                    else
                        messages.add(utils.isMessageSent(item) ? "You : [\uD83C\uDFA5] ---[ Shared a post ] ---" : username + " : [\uD83C\uDFA5] ---[ Shared a post ] ---");
                }
                Collections.reverse(messages);
                return messages;
            });
        }

        /**
         * Retrieve messages from a particular chat from a particular message till last.
         * @param username The user to get the message from.
         * @param fromMessage The message from where to start fetching messages. This is case-sensitive
         * @param frequency   If 'fromMessage' is occurred multiple time, then pass the no. from where to consider.
         *                    For example, To retrieve messages from 'hey' and if there are 3 more same messages (exact equal) like 'hey'
         *                    then pass 1 to get from last 'hey' or 2 or 3 from the top second or first 'hey'.
         * @return A {@link AsyncTask} holding list of messages.
         * @throws IllegalArgumentException if frequency is negative.
         */
        public AsyncTask<List<String>> getChatMessagesFrom(@NotNull String username,@NotNull String fromMessage, int frequency) {
            return AsyncTask.callAsync(() -> {
                List<String> messages = new ArrayList<>();
                List<ThreadItem> items = utils.getThreadItem(username, fromMessage, frequency, false, null);
                for (ThreadItem item : items) {
                    if (item instanceof ThreadTextItem)
                        messages.add(utils.isMessageSent(item) ? "You : " + ((ThreadTextItem) item).getText().replace("\n", "") : username + " : " + ((ThreadTextItem) item).getText().replace("\n", ""));
                    else
                        messages.add(utils.isMessageSent(item) ? "You : [\uD83C\uDFA5] ---[ Shared a post ] ---" : username + " : [\uD83C\uDFA5] ---[ Shared a post ] ---");
                }
                Collections.reverse(messages);
                return messages;
            });
        }


        /**
         * Broadcast a text message in a direct chat group.
         *
         * @param message       The message to broadcast.
         * @param adminUsername The username of any one admin of the group. If the same user is admin of 2 or more group then the top first
         *                      group will be selected.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        @NotNull
        public AsyncTask<Void> groupMessage(@NotNull String message, @NotNull String adminUsername) {
            return AsyncTask.callAsync(() -> {
                List<IGThread> threads = client.sendRequest(new DirectInboxRequest()).get().getInbox().getThreads();
                Long pk = client.actions().users().findByUsername(adminUsername).get().getUser().getPk();
                for (IGThread thread : threads) {
                    if (thread.is_group() && thread.getAdmin_user_ids().contains(String.valueOf(pk))) {
                        DirectThreadsBroadcastRequest request = new DirectThreadsBroadcastRequest(new DirectThreadsBroadcastRequest.BroadcastTextPayload(message, thread.getThread_id()));
                        utils.request(request);
                        return null;
                    }
                }
                throw new InstagramException(adminUsername + " is not admin of any group in your inbox", Reasons.NO_SUCH_ADMIN);
            });
        }


        /**
         * Creates a Instagram direct group with the given members. Unimplemented.
         *
         * @param groupName The title of the group
         * @param usernames The usernames of the members to add in the group.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        @NotNull
        private AsyncTask<Void> createGroup(@NotNull String groupName, @NotNull String... usernames) {
            String[] pks = new String[usernames.length];
            return AsyncTask.callAsync(() -> {
                try {
                    for (int i = 0; i < pks.length; i++) {
                        pks[i] = String.valueOf(client.actions().users().findByUsername(usernames[i]).get().getUser().getPk());
                    }
                    DirectCreateGroupThreadRequest request = new DirectCreateGroupThreadRequest(groupName, pks);
                    utils.request(request);
                    return null;
                } catch (ExecutionException | InterruptedException e) {
                    throw new InstagramException("One of the username is not valid.", Reasons.INVALID_USERNAME);
                }
            });
        }


        /**
         * Send mass messages to a user at a single time (also called spamming)
         *
         * @param count    The no. of message to sent
         * @param messages list of String containing messages to sent. a random message picked will be sent from this list.
         * @param callback Callback indication success and failure of each message.
         */
        public void spamDM(@NotNull String username,int count, @NotNull String[] messages, @NotNull OnMessageActionCallback callback) {
            for (int i = 0; i < count; i++) {
                String message = messages[new Random().nextInt(messages.length)];
                callback.onProgress(i * 100 / messages.length);
                directMessage(username,message).setOnCompleteCallback(task -> {
                    if (task.isSuccessful)
                        callback.onSuccess(message);
                    else
                        callback.onFailed(task.exception);
                });
            }
        }


        /**
         * Send mass messages to a group at a single time (also called spamming)
         *
         * @param count         The no. of message to sent
         * @param adminUsername Username of admin of the group. If this user is also admin of another group, then the Top first group in your inbox will be considered.
         * @param messages      list of String containing messages to sent. a random message picked will be sent from this list.
         * @param callback      Callback indication success and failure of each message.
         */
        public void spamGroup(int count, @NotNull String adminUsername, @NotNull String[] messages, @NotNull OnMessageActionCallback callback) {
            for (int i = 0; i < count; i++) {
                String message = messages[new Random().nextInt(messages.length)];
                callback.onProgress(i * 100 / count);
                groupMessage(message,adminUsername).setOnCompleteCallback(task -> {
                    if (task.isSuccessful)
                        callback.onSuccess(message);
                    else
                        callback.onFailed(task.exception);
                });
            }

        }

        /**
         * Deletes the messages from the last till the particular number.
         * @param username The user to to delete messages from.
         * @param howMany  The no. of messages (including recipient message) to delete. For example,
         *                 if there are last 2 message from him/her and then 3 messages from you,
         *                 so you have to pass 5 here to delete your 3 messages. Pass 0 to delete all.
         * @param callback The callback indication success or failure of each delete message request.
         * @throws IllegalArgumentException if howMany is negative.
         */
        public void deleteMessages(@NotNull String username,int howMany, @NotNull OnMessageActionCallback callback) {
            try {
                String threadId = utils.getThread(username).getThread_id();
                List<ThreadItem> items = utils.getThreadItem(username, howMany, true, null);
                for (int i = 0; i < items.size(); i++) {
                    ThreadItem item = items.get(i);
                    callback.onProgress(i * 100 / items.size());
                    utils.request(new DirectThreadsDeleteItemRequest(threadId, item.getItem_id()));
                }
                callback.onSuccess(null);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                e.printStackTrace();
                callback.onFailed(e);
            }
        }

        /**
         * Starts deleting the messages from the exact 'message' that was sent bu you.
         * @param username The user to to delete messages from.
         * @param fromMessage The message to start deleting from.
         * @param frequency if the 'fromMessage' occurred more then 1 time, pass the number from where to consider. Otherwise pass 0.
         * @param callback The callback indication success,failure and progress of the operation.
         */
        public void deleteMessagesFrom(@NotNull String username,@NotNull String fromMessage, int frequency, @Nullable OnMessageActionCallback callback) {
            callback = callback == null ? new OnMessageActionCallback() {
                @Override
                public void onSuccess(String message) {

                }

                @Override
                public void onFailed(Exception exception) {

                }

                @Override
                public void onProgress(int percentage) {

                }
            } : callback;
            try {
                List<ThreadItem> items = utils.getThreadItem(username, fromMessage, frequency, true, null);
                String threadId = utils.getThread(username).getThread_id();
                for (int i = 0; i < items.size(); i++) {
                    ThreadItem item = items.get(i);
                    utils.request(new DirectThreadsDeleteItemRequest(threadId, item.getItem_id()));
                    callback.onProgress(i * 100 / items.size());
                }
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onFailed(e);
            }
        }

        /**
         * Starts listening for new messages.
         * @param listener The listener to call when new messages are received.
         */
        public void attachNotificationListener(@NotNull OnNotificationListener listener) {
            IGRealtimeClient realtimeClient = new IGRealtimeClient(client, packet -> {
                try {
                    if (packet instanceof PublishPacket message && message.topicName.equals("146")){
                        var payload = PacketUtil.stringify(ZipUtil.unzip(message.getPayload()));
                        JsonNode data = IGUtils.jsonToObject(payload, JsonNode.class).get(0).get("data").get(0);
                        var value = IGUtils.jsonToObject(data.get("value").asText(), JsonNode.class);
                        if (data.get("op").asText().equals("add")){
                            var user_id = value.get("user_id").asLong();
                            if (user_id != client.getSelfProfile().getPk()){
                                var thread_id = data.get("path").asText().substring(1).split("/")[2];
                                var item_id = value.get("item_id").asText();
                                var text = value.get("text").asText();
                                listener.onMessage(new Message(client,text,thread_id, item_id,user_id));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread = new Thread(realtimeClient::connect);
            thread.start();
        }

        /**
         * Stops listening for new messages.
         * @throws NullPointerException if the listener was never attached.
         */
        public void detectNotificationListener() {
            thread.stop();
        }
    }


    /**
     * This class includes all the methods related to profile of the user.
     */
    public final class Profile {

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
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> setBio(@NotNull String bio) {
            if (bio.length() > 150)
                throw new InstagramException("Bio must be of less then 150 characters", Reasons.BIO_LENGTH_EXCEEDED);

            return AsyncTask.callAsync(() -> {
                client.actions().account().setBio(bio).get(5, TimeUnit.SECONDS);
                return null;
            });
        }


        /**
         * Scraps 100 followers of the account associated with the username. Empty list if none found but Never null.
         * @param username The user to get the followers from.
         * @return A {@link AsyncTask} holding list of the followers.
         */
        public @NotNull
        AsyncTask<List<String>> getFollowers(@NotNull String username) {
            return AsyncTask.callAsync(() -> utils.getFeeds(client.actions().users().findByUsername(username), true));
        }


        /**
         * Scraps 100 followings of the account associated with the provided username. Empty list if none found but Never null.
         * @param username The user to get the followings from.
         * @return A {@link AsyncTask} holding list of the followings.
         */
        public AsyncTask<List<String>> getFollowings(@NotNull String username) {
            return AsyncTask.callAsync(() -> utils.getFeeds(client.actions().users().findByUsername(username), false));
        }


        /**
         * Gets the pending follow requests of yours.
         * @return A {@link AsyncTask} holding usernames of the requests.
         */
        public AsyncTask<List<String>> getFollowRequests() {
            return AsyncTask.callAsync(() -> {
                List<String> usernames = new ArrayList<>();
                FeedUsersResponse feedUsersResponse = client.sendRequest(new FriendshipsPendingRequest()).get(5, TimeUnit.SECONDS);
                for (com.github.instagram4j.instagram4j.models.user.Profile user : feedUsersResponse.getUsers()) {
                    usernames.add(user.getUsername());
                }
                return usernames;
            });
        }


        /**
         * Gets the bio of the account associated with the provided username.
         * @param username The user to get the bio from.
         * @return A {@link AsyncTask} holding bio of the user.
         */
        public AsyncTask<String> getBio(@NotNull String username) {
            return AsyncTask.callAsync(() -> utils.getUser(client.actions().users().findByUsername(username)).getBiography());
        }


        /**
         * Gets the profile photo link of the account associated with the username provided.
         * @param username The user to get the DP from.
         * @return A {@link AsyncTask} holding url of profile picture of the user.
         */
        public AsyncTask<String> getProfilePicUrl(@NotNull String username) {
            return AsyncTask.callAsync(() -> utils.getUser(client.actions().users().findByUsername(username)).getProfile_pic_url());
        }


        /**
         * Gets the follower count of the account associated with the username.
         * @param username The user to get the followers from.
         * @return A {@link AsyncTask} holding number of followers of the user.
         */
        public AsyncTask<Integer> getFollowersCount(@NotNull String username) {
            return AsyncTask.callAsync(() -> utils.getUser(client.actions().users().findByUsername(username)).getFollower_count());
        }


        /**
         * Gets the followings count of the account associated with the username provided.
         * @param username The user to get the followings from.
         * @return A {@link AsyncTask} holding number of followings of the user.
         */
        public AsyncTask<Integer> getFollowingsCount(@NotNull String username) {
            return AsyncTask.callAsync(() -> utils.getUser(client.actions().users().findByUsername(username)).getFollowing_count());
        }


        /**
         * Gets the post count of the account associated with the username provided.
         * @param username The user to get the posts count from.
         * @return A {@link AsyncTask} holding number of posts of the user.
         */
        public AsyncTask<Integer> getPostCount(@NotNull String username) {
            return AsyncTask.callAsync(() -> utils.getUser(client.actions().users().findByUsername(username)).getMedia_count());
        }
    }


    /**
     * This class includes all the methods related to your feed.
     */
    public final class Feed {
        /**
         * Post a new picture.
         *
         * @param photo   The photo to post
         * @param caption Caption for the post
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> postPhoto(@NotNull File photo, String caption) {
            if (photo.getName().endsWith(".mp4"))
                throw new InstagramException("Uploading or sending video is not currently supported", Reasons.UNSUPPORTED_FILE_FORMAT);

            return AsyncTask.callAsync(() -> {
                client.actions().timeline().uploadPhoto(photo, caption == null ? "Uploaded using EasyInsta library" : caption).get(5, TimeUnit.SECONDS);
                return null;
            });
        }


        /**
         * Uploads a photo to your story.
         *
         * @param photo The photo to add as story.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> addStory(@NotNull File photo) {
            if (photo.getName().endsWith(".mp4"))
                throw new InstagramException("Uploading or sending video is not currently supported", Reasons.UNSUPPORTED_FILE_FORMAT);

            return AsyncTask.callAsync(() -> {
                client.actions().story().uploadPhoto(photo).get(5, TimeUnit.SECONDS);
                return null;
            });
        }
    }


    /**
     * This class includes all common action of your instagram, like following someone,accepting follow request,removing followers etc.
     */
    public final class Actions {


        /**
         * Follows the user.
         *
         * @param username Username of the user to follow.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> follow(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.CREATE);
                return null;
            });
        }


        /**
         * Unfollows the user.
         *
         * @param username Username of the user to unfollow.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> unfollow(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.DESTROY);
                return null;
            });
        }


        /**
         * Accept the pending follow request, if had any.
         *
         * @param username Username of the user who had requested.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> accept(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.APPROVE);
                return null;
            });
        }


        /**
         * Ignores the pending follow request, if had any.
         *
         * @param username Username of the user who had requested.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> ignore(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.IGNORE);
                return null;
            });
        }


        /**
         * Remove the user from your followers.
         *
         * @param username Username of the user to remove.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> removeFollower(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                utils.followAction(username, FriendshipsActionRequest.FriendshipsAction.REMOVE_FOLLOWER);
                return null;
            });
        }
    }
}