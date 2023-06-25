package com.xcoder.easyinsta.models;

import port.org.json.JSONObject;

public class UserInfo {
    public String username;
    public String fullName;
    public String biography;
    public String profilePicUrl;
    public int followers;
    public int following;
    public int posts;

    public UserInfo(String username, String name, String bio, String dp, int followers, int following, int posts) {
        this.username = username;
        this.fullName = name;
        this.biography = bio;
        this.profilePicUrl = dp;
        this.followers = followers;
        this.following = following;
        this.posts = posts;
    }

    public UserInfo(JSONObject json){
        this.biography = json.optString("biography");
        this.followers = json.getJSONObject("edge_followed_by").getInt("count");
        this.following = json.getJSONObject("edge_follow").getInt("count");
        this.fullName = json.getString("full_name");
        this.posts = json.getJSONObject("edge_owner_to_timeline_media").getInt("count");
        this.profilePicUrl = json.getString("profile_pic_url_hd");
    }
}
