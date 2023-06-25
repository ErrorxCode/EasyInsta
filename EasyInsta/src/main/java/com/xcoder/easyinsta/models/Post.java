package com.xcoder.easyinsta.models;

import com.github.instagram4j.instagram4j.actions.story.StoryAction;
import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;
import com.github.instagram4j.instagram4j.requests.media.MediaActionRequest;
import com.github.instagram4j.instagram4j.requests.media.MediaCommentRequest;
import com.xcoder.easyinsta.Instagram;
import com.xcoder.easyinsta.Utils;
import com.xcoder.tasks.AsyncTask;

import org.jetbrains.annotations.NotNull;


public class Post extends PostInfo {
    private final Utils utils;

    public Post(Utils utils, TimelineMedia media) {
        super(media);
        this.utils = utils;
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
