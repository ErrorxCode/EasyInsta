package com.xcoder.easyinsta.models;

public class User {
    public String username;
    public String fullName;
    public String biography;
    public String profilePicUrl;
    public int followers;
    public int following;
    public int posts;

    public User(String username, String name, String bio, String dp, int followers, int following, int posts) {
        this.username = username;
        this.fullName = name;
        this.biography = bio;
        this.profilePicUrl = dp;
        this.followers = followers;
        this.following = following;
        this.posts = posts;
    }
}
