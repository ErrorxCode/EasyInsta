package com.xcoder.easyinsta.models;

import com.github.instagram4j.instagram4j.actions.story.StoryAction;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;
import com.github.instagram4j.instagram4j.requests.media.MediaActionRequest;
import com.github.instagram4j.instagram4j.requests.media.MediaCommentRequest;
import com.xcoder.easyinsta.Instagram;
import com.xcoder.easyinsta.Utils;
import com.xcoder.tasks.AsyncTask;

import org.jetbrains.annotations.NotNull;


public class Post {
    private Utils utils;
    public String id;
    public int likes;
    public int comments;
    public String caption;
    public String link;
    public String owner;

    public Post(Utils utils, TimelineMedia media) {
        this.utils = utils;
        this.id = media.getId();
        this.likes = media.getLike_count();
        this.comments = media.getComment_count();
        this.caption = media.getCaption().getText();
        this.link = "https://instagram.com/p/" + media.getCode();
        this.owner = media.getUser().getUsername();
    }

    public AsyncTask<Void> like(){
        return AsyncTask.callAsync(() -> {
            utils.request(new MediaActionRequest(id, MediaActionRequest.MediaAction.LIKE));
            return null;
        });
    }

    public AsyncTask<Void> comment(@NotNull String comment){
        return AsyncTask.callAsync(() -> {
            utils.request(new MediaCommentRequest(id, comment));
            return null;
        });
    }

    @Override
    public String toString() {
        return "Post{" +
                "utils=" + utils +
                ", id='" + id + '\'' +
                ", likes=" + likes +
                ", comments=" + comments +
                ", caption='" + caption + '\'' +
                ", link='" + link + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
