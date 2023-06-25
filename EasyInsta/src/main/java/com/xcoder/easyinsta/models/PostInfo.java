package com.xcoder.easyinsta.models;

import com.github.instagram4j.instagram4j.models.media.timeline.TimelineMedia;

import port.org.json.JSONObject;

public class PostInfo {
    public String id;
    public int likes;
    public int comments;
    public String caption;
    public String link;
    public String downloadURL;
    public String owner;
    public String shortCode;


    public PostInfo(TimelineMedia media) {
        this.id = media.getId();
        this.likes = media.getLike_count();
        this.comments = media.getComment_count();
        this.caption = media.getCaption().getText();
        this.link = "https://instagram.com/p/" + media.getCode();
        this.owner = media.getUser().getUsername();
        this.shortCode = media.getCode();
    }
    public PostInfo(JSONObject media){
        this.id = media.getString("id");
        this.shortCode = media.getString("shortcode");
        this.downloadURL = media.getString("display_url");
        this.likes = media.getJSONObject("edge_media_preview_like").getInt("count");
        this.comments = media.getJSONObject("edge_media_to_comment").getInt("count");
        this.caption = media.getJSONObject("edge_media_to_caption").getJSONArray("edges").getJSONObject(0).getJSONObject("node").getString("text");
        this.link = "https://instagram.com/p/" + shortCode;
        this.owner = media.getString("owner");
    }
}
