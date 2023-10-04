package com.xcoder.easyinsta;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.direct.IGThread;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadItem;
import com.github.instagram4j.instagram4j.models.direct.item.ThreadTextItem;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;
import com.github.instagram4j.instagram4j.requests.direct.DirectCreateGroupThreadRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectInboxRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsBroadcastRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsDeleteItemRequest;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsMarkItemSeenRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserStoryRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsPendingRequest;
import com.github.instagram4j.instagram4j.responses.feed.FeedUsersResponse;
import com.github.instagram4j.instagram4j.responses.media.MediaResponse;
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
import com.xcoder.easyinsta.models.Message;
import com.xcoder.easyinsta.models.Post;
import com.xcoder.easyinsta.models.PostInfo;
import com.xcoder.easyinsta.models.UserInfo;
import com.xcoder.tasks.AsyncTask;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import port.org.json.JSONObject;

/**
 * This is an easy implementation of instagram private api in java. This class include various methods to perform instagram operation like
 * direct messages,adding stories,posting photos,scrapping profile data like posts,followers,followings etc. All the methods runs asynchronously.
 *
 * @author Rahil khan
 * @version 1.5
 */
public class Instagram {
    public final IGClient client;
    private final Utils utils;
    public String username;
    public String password;
    protected static Instagram insta;
    protected static String verificationCode;
    public static final int MESSAGE_ALL = 0;
    public static final int FREQUENCY_ALL = 0;
    public static final int FREQUENCY_FIRST = 1;
    public static final int FREQUENCY_SECOND = 2;
    public static final int FREQUENCY_THIRD = 3;

    private Instagram(IGClient client, String password) {
        this.client = client;
        utils = new Utils(this);
        this.password = password;
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
            return new Instagram(builder.login(), password);
        } catch (com.github.instagram4j.instagram4j.exceptions.IGLoginException e) {
            e.printStackTrace();
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
            else if (username.isBlank() || password.isBlank())
                throw new IGLoginException("Username and password is blank", Reasons.INVALID_CREDENTIALS);
            else
                throw new IGLoginException("An unknown error occurred. This must not happen in most of the case. If this problem persist, " +
                        "then please create a issue on github regarding this exception.\nStack trace :-\n" + e.getLocalizedMessage(), Reasons.LOGIN_ERROR_UNKNOWN);
        }
    }


    /**
     * Notice : This method may not work correctly in future as instagram keep changing their verification process and API urls.
     * So it's recommended not to rely on this method as it can break your code.
     * Loges in to your instagram account with the provided username and password for 2FA enabled account. You have to call {@link #verify2factor(String)}
     * passing verification code to complete the login flow.
     *
     * @param username Username of the account to log in.
     * @param password Password of the account associated with this username.
     * @param callback The callback that will be invoked when verification code is sent. This will wait until you call. You have to return the verification code.
     */
    public static void login2factor(@NotNull String username, @NotNull String password, @NotNull Callable<String> callback) {
        new Thread(() -> {
            try {
                insta = new Instagram(IGClient.builder()
                        .username(username)
                        .password(password)
                        .onTwoFactor((client, t) -> IGChallengeUtils.resolveTwoFactor(client, t, new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                verificationCode.wait();
                                return verificationCode;
                            }
                        })).login(), password);
            } catch (com.github.instagram4j.instagram4j.exceptions.IGLoginException e) {
                e.printStackTrace();
                try {
                    if (e.getMessage().contains("few minutes"))
                        throw new IGLoginException("Please wait for few minutes before trying again. Instagram blocks requests that are coming from same IP", Reasons.LOGIN_TOO_FREQUENTLY);
                    else if (e.getMessage().contains("password"))
                        throw new IGLoginException("Username or password is incorrect", Reasons.INVALID_CREDENTIALS);
                    else if (e.getMessage().contains("challenge"))
                        throw new IGLoginException("You account is temporary suspended by instagram. Open https://i.instagram.com/challenge to verify your account.", Reasons.CHALLENGE_REQUIRED);
                    else
                        throw new IGLoginException("An unknown error occurred. This must not happen in most of the case. If this problem persist, then please create a issue on github regarding this exception", Reasons.LOGIN_ERROR_UNKNOWN);
                } catch (IGLoginException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }).start();
    }

    public static Instagram verify2factor(@NotNull String code) {
        verificationCode = code;
        verificationCode.notify();
        return insta;
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
            File client = new File(dir, "ClientObject.ser");
            File cookie = new File(dir, "LoginSession.ser");
            if (client.exists() && cookie.exists())
                return new Instagram(IGClient.deserialize(client, cookie), password);
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
     * Caches the current instagram session, Overwrite if already exist. There is no guarantee that it will always cache. It may fail sometimes (0.1% chance)
     *
     * @param dir The cache directory of the application.
     * @return true if the cache was successful. false otherwise.
     */
    public boolean cache(@NotNull File dir) {
        File client = new File(dir, "ClientObject.ser");
        File cookie = new File(dir, "LoginSession.ser");
        try {
            this.client.serialize(client, cookie);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void relogin() throws IGLoginException {
        insta = login(username, password);
    }

    /**
     * Returns weather the current session is been cached or not. If a session is cached then
     * it is considered logged in, else not.
     *
     * @param dir Directory where cache is saved
     * @return true if logged in,false otherwise
     */
    public static boolean isLogin(File dir) {
        return new File(dir, "LoginSession.ser").exists();
    }

    /**
     * Clears the instagram cache if exist.
     *
     * @param dir The directory where cache is stored.
     */
    public void clearCache(@NotNull File dir) {
        File client = new File(dir, "ClientObject.ser");
        File cookie = new File(dir, "LoginSession.ser");
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
        private IGRealtimeClient realtimeClient;

        /**
         * Sends a direct message to a user
         *
         * @param message  The text to send.
         * @param username The user to send the message to.
         * @return A {@link AsyncTask} indication success or failure of the method
         */
        public AsyncTask<Void> directMessage(@NotNull String username, @NotNull String message) {
            return AsyncTask.callAsync(() -> {
                long user = client.actions().users().findByUsername(username).get().getUser().getPk();
                DirectThreadsBroadcastRequest.BroadcastPayload payload;
                if (message.contains("http"))
                    payload = new DirectThreadsBroadcastRequest.BroadcastLinkPayload(message, Utils.extractUrls(message), user);
                else
                    payload = new DirectThreadsBroadcastRequest.BroadcastTextPayload(message, user);

                utils.request(new DirectThreadsBroadcastRequest(payload));
                return null;
            });
        }


        /**
         * Sends a photo to the user.
         *
         * @param photo    The photo to send.
         * @param username The user to send the message to.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> directMessage(@NotNull String username, @NotNull File photo) {
            if (!photo.getName().endsWith(".jpg") && !photo.getName().endsWith(".png") && !photo.getName().endsWith("jpeg"))
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
         *
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
        public AsyncTask<List<String>> getChatMessages(@NotNull String username, int howMany, @Nullable OnProgressListener listener) {
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
         *
         * @param username    The user to get the message from.
         * @param fromMessage The message from where to start fetching messages. This is case-sensitive
         * @param frequency   If 'fromMessage' is occurred multiple time, then pass the no. from where to consider.
         *                    For example, To retrieve messages from 'hey' and if there are 3 more same messages (exact equal) like 'hey'
         *                    then pass 1 to get from last 'hey' or 2 or 3 from the top second or first 'hey'.
         * @return A {@link AsyncTask} holding list of messages.
         * @throws IllegalArgumentException if frequency is negative.
         */
        public AsyncTask<List<String>> getChatMessagesFrom(@NotNull String username, @NotNull String fromMessage, int frequency) {
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
        public void spamDM(@NotNull String username, int count, @NotNull String[] messages, @NotNull OnMessageActionCallback callback) {
            for (int i = 0; i < count; i++) {
                String message = messages[new Random().nextInt(messages.length)];
                callback.onProgress(i * 100 / messages.length);
                directMessage(username, message).setOnCompleteCallback(task -> {
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
                groupMessage(message, adminUsername).setOnCompleteCallback(task -> {
                    if (task.isSuccessful)
                        callback.onSuccess(message);
                    else
                        callback.onFailed(task.exception);
                });
            }

        }

        /**
         * Deletes the messages from the last till the particular number.
         *
         * @param username The user to to delete messages from.
         * @param howMany  The no. of messages (including recipient message) to delete. For example,
         *                 if there are last 2 message from him/her and then 3 messages from you,
         *                 so you have to pass 5 here to delete your 3 messages. Pass 0 to delete all.
         * @param callback The callback indication success or failure of each delete message request.
         * @throws IllegalArgumentException if howMany is negative.
         */
        public void deleteMessages(@NotNull String username, int howMany, @NotNull OnMessageActionCallback callback) {
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
         *
         * @param username    The user to to delete messages from.
         * @param fromMessage The message to start deleting from.
         * @param frequency   if the 'fromMessage' occurred more then 1 time, pass the number from where to consider. Otherwise pass 0.
         * @param callback    The callback indication success,failure and progress of the operation.
         */
        public void deleteMessagesFrom(@NotNull String username, @NotNull String fromMessage, int frequency, @Nullable OnMessageActionCallback callback) {
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
         *
         * @param listener The listener to call when new messages are received.
         */
        public void attachNotificationListener(@NotNull OnNotificationListener listener) {
            realtimeClient = new IGRealtimeClient(client, packet -> {
                try {
                    if (packet instanceof PublishPacket message && message.topicName.equals("146")) {
                        var payload = PacketUtil.stringify(ZipUtil.unzip(message.getPayload()));
                        JsonNode data = IGUtils.jsonToObject(payload, JsonNode.class).get(0).get("data").get(0);
                        var value = IGUtils.jsonToObject(data.get("value").asText(), JsonNode.class);
                        if (data.get("op").asText().equals("add")) {
                            var user_id = value.get("user_id").asLong();
                            if (user_id != client.getSelfProfile().getPk()) {
                                var thread_id = data.get("path").asText().substring(1).split("/")[2];
                                var item_id = value.get("item_id").asText();
                                var type = value.get("item_type").asText();
                                var text = type.equals("text") ? value.get("text").asText() : value.get("link").get("text").asText();
                                var sender = client.actions().users().info(user_id).get(5, TimeUnit.SECONDS).getUsername();
                                listener.onMessage(new Message(Instagram.this, text, sender));
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
         *
         * @throws NullPointerException if the listener was not attached.
         */
        public void detectNotificationListener() {
            try {
                thread.stop();
            } catch (UnsupportedOperationException e) {
                try {
                    realtimeClient.disconnect();
                } catch (IOException error) {
                    error.printStackTrace();
                    realtimeClient = null;
                }
            }
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
         *
         * @param username The user to get the followers from.
         * @return A {@link AsyncTask} holding list of the followers.
         */
        public AsyncTask<List<String>> getFollowers(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                var list = new ArrayList<String>();
                utils.getUser(username).followersFeed().iterator().next().getUsers().forEach(profile -> list.add(profile.getUsername()));
                return list;
            });
        }


        /**
         * Scraps 100 followings of the account associated with the provided username. Empty list if none found but Never null.
         *
         * @param username The user to get the followings from.
         * @return A {@link AsyncTask} holding list of the followings.
         */
        public AsyncTask<List<String>> getFollowings(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                var list = new ArrayList<String>();
                utils.getUser(username).followingFeed().iterator().next().getUsers().forEach(profile -> list.add(profile.getUsername()));
                return list;
            });
        }


        /**
         * Gets the pending follow requests of yours.
         *
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
         * Gets the user info of the provided username.
         *
         * @param username The username to get the info from.
         * @return A {@link AsyncTask} holding the user info.
         */
        public AsyncTask<UserInfo> getUserInfo(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                try {
                    return PublicAPI.getUser(username).getResult(5);
                } catch (Exception e) {
                    var data = utils.getUser(username).getUser();
                    return new UserInfo(data.getUsername(), data.getFull_name(), data.getBiography(), data.getProfile_pic_url(), data.getFollower_count(), data.getFollowing_count(), data.getMedia_count());
                }
            });
        }


        public AsyncTask<Post[]> getPosts(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                var pk = utils.getUser(username).getUser().getPk();
                var items = utils.request(new FeedUserRequest(pk)).getItems();
                Post[] posts = new Post[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    TimelineMedia item = items.get(i);
                    posts[i] = new Post(utils, item);
                }
                return posts;
            });
        }
    }

    // TODO: 18-06-2023 Implement get post by url and get post of user


    /**
     * This class includes all the methods related to your feed.
     */
    public final class Feed {
        /**
         * Create a new post
         *
         * @param post    The photo or video to post
         * @param caption Caption for the post
         * @return A {@link AsyncTask} indication success or failure of the request with a possible return object
         */
        public AsyncTask<MediaResponse.MediaConfigureTimelineResponse> post(@NotNull File post, String caption) {
            return AsyncTask.callAsync(() -> {
                MediaResponse.MediaConfigureTimelineResponse retVal;
                var timeline = client.actions().timeline();
                if (post.getName().endsWith(".mp4")) {
                    var stream = new URL("https://docs.clorabase.tk/favicon.png").openStream();
                    var file = File.createTempFile("clorabase-logo", ".png");
                    Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    retVal = timeline.uploadVideo(post, file, caption).get(60, TimeUnit.SECONDS);
                } else
                    retVal = timeline.uploadPhoto(post, caption).get(10, TimeUnit.SECONDS);
                return retVal;
            });
        }


        /**
         * Uploads a photo to your story.
         *
         * @param story The photo to add as story.
         * @return A {@link AsyncTask} indication success or failure of the request
         */
        public AsyncTask<Void> addStory(@NotNull File story) {
            return AsyncTask.callAsync(() -> {
                var timeline = client.actions().story();
                if (story.getName().endsWith(".mp4")) {
                    var stream = new URL("https://docs.clorabase.tk/favicon.png").openStream();
                    var file = File.createTempFile("clorabase-logo", ".png");
                    Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    timeline.uploadVideo(story, file).get(60, TimeUnit.SECONDS);
                } else
                    timeline.uploadPhoto(story).get(10, TimeUnit.SECONDS);
                return null;
            });
        }

        /**
         * Gets the number of views of your story.
         *
         * @return A {@link AsyncTask} holding number of views of your story.
         */
        public AsyncTask<Integer> getStoryViews() {
            return AsyncTask.callAsync(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    var pk = client.getSelfProfile().getPk();
                    var response = utils.request(new FeedUserStoryRequest(pk));
                    return response.getReel().getItems().get(0).getTotal_viewer_count();
                }
            });
        }

        /**
         * Get's the maximum of 9-10 posts of yourself
         *
         * @return List of posts
         */
        public AsyncTask<List<Post>> getLatestPosts() {
            return AsyncTask.callAsync(() -> {
                var list = new ArrayList<Post>();
                client.actions().timeline().feed().stream().limit(2).forEach(response -> {
                    var items = response.getFeed_items();
                    for (TimelineMedia media : items) {
                        list.add(new Post(utils, media));
                    }
                });
                return list;
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

    /**
     * This class is a collection of instagram public apis. These api only works on public accounts
     */
    public static class PublicAPI {
        private static final String POST_ID = "17888483320059182";
        private static final String FOLLOWING_ID = "17874545323001329";
        private static final String FOLLOWERS_ID = "17851374694183129";
        private static final String GRAPH_URL = "https://www.instagram.com/graphql/query/?query_id=";

        public static AsyncTask<PostInfo[]> getPosts(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                var variables = new JSONObject();
                variables.put("id", Utils.getPkFromUsername(username));
                variables.put("first", 50);
                var url = GRAPH_URL + POST_ID + "&variables=" + variables;
                var json = Utils.fetchDataNode(url, null).getJSONObject("user").getJSONObject("edge_owner_to_timeline_media");
                var edges = json.getJSONArray("edges");
                PostInfo[] posts = new PostInfo[edges.length()];
                for (int i = 0; i < edges.length(); i++) {
                    var node = edges.getJSONObject(i).getJSONObject("node");
                    posts[i] = new PostInfo(node);
                }
                return posts;
            });
        }

        public static AsyncTask<PostInfo> getPost(@NotNull String link) {
            return AsyncTask.callAsync(() -> {
                var code = Utils.getMediaCodeFromURL(link);
                var url = String.format("https://www.instagram.com/graphql/query/?query_hash=b3055c01b4b222b8a47dc12b090e4e64&variables={\"shortcode\":\"%s\"}", code);
                return new PostInfo(Utils.fetchDataNode(url,null).getJSONObject("shortcode_media"));
            });
        }

        public static AsyncTask<UserInfo> getUser(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                var url = "https://www.instagram.com/api/v1/users/web_profile_info/?username=" + username;
                var json = Utils.fetchDataNode(url, null).getJSONObject("user");
                return new UserInfo(json);
            });
        }

        public static AsyncTask<String> getProfilePictureURL(@NotNull String username) {
            return AsyncTask.callAsync(() -> getUser(username).getResult(5).profilePicUrl);
        }

        public static AsyncTask<String> getPostDownloadURL(@NotNull String link){
            return AsyncTask.callAsync(() -> getPost(link).getResult(5).downloadURL);
        }

        @Deprecated
        /**
         * Not working right now
         */
        public AsyncTask<List<String>> getFollowers(@NotNull String username) {
            return AsyncTask.callAsync(() -> {
                var variables = new JSONObject();
                variables.put("first", 200);
                variables.put("id", Utils.getPkFromUsername(username));
                var url = GRAPH_URL + FOLLOWERS_ID + "&variables=" + variables;
                var json = Utils.fetchDataNode(url, null).getJSONObject("user").getJSONObject("edge_followed_by");
                var edges = json.getJSONArray("edges");
                var followers = new ArrayList<String>();
                for (int i = 0; i < edges.length(); i++) {
                    var node = edges.getJSONObject(i).getJSONObject("node");
                    System.out.println(node);
                    followers.add(node.getString("username"));
                }
                return followers;
            });
        }

        @Deprecated
        /**
         * Not working right now
         */
        public AsyncTask<List<String>> getFollowings(@NotNull String username, String cookie) {
            return AsyncTask.callAsync(() -> {
                var variables = new JSONObject();
                variables.put("first", 200);
                variables.put("id", Utils.getPkFromUsername(username));
                var url = GRAPH_URL + FOLLOWING_ID + "&variables=" + variables;
                var json = Utils.fetchDataNode(url, cookie).getJSONObject("user").getJSONObject("edge_follow");
                var edges = json.getJSONArray("edges");
                var followings = new ArrayList<String>();
                for (int i = 0; i < edges.length(); i++) {
                    var node = edges.getJSONObject(i).getJSONObject("node");
                    followings.add(node.getString("username"));
                }
                return followings;
            });
        }
    }
}
