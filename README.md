
# EasyInsta
This is an android library through which you can use instagram programatically. You can say that this is a well optimize, will featured java wrapper of "Instagram graph API". You can direct messages, add stories, post photos, scrapping profiles and can do many more things with this library.

![Banner](https://vinyl-state.com/wp-content/uploads/2020/12/instagram-logo2.jpg)

## Features

- Lightweight and Easy 2 use
- No need api token
- Supports **sending direct message** (Text & photos)
- Supports **posting** (Only photo)
- Supports **adding stories** (Only photo)
- Supports **Following / Unfollowing others**
- Supports **Acception / Ignoring follow request**
- Supports **Scrapping followings and followers**

## Implimentation
 In your project build.gradle
```groovy
 allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
In your app build.gradle
```groovy
dependencies {
	        implementation 'com.github.ErrorxCode:EasyInsta:1'
	}
```



## Acknowledgements

 - [Instagram graph api](https://developers.facebook.com/docs/instagram-api/)
 - [API Policies](https://developers.facebook.com/devpolicy/)


## API Reference

#### Create instagram & callback object
```
Instagram insta = new Instagram("username","password");
OnCompleteCallback callback = new OnCompleteCallback() {
            @Override
            public void onSuccess() {
                System.out.println("Operation successful");
            }

            @Override
            public void onFailed(Throwable e) {
                e.printStackTrace();
                System.out.println("Operation failed");
            }
        };
```

#### Now let's post something...
```
insta.addStory(new File("story.png"),callback);
insta.post(new File("post.png"),"This is caption",callback);
```

#### Send direct message
```
insta.directMessage("x__coder__x","This is message...",null);
insta.directMessage("x__coder__x",new File("photo.jpg"),callback);
```

#### Follow / Unfollow / remove someone
```
insta.follow("username2follow",callback);
insta.unfollow("username2unfollow",callback);
insta.removeFollower("username2remove",callback);

```

#### Accept or ignore follow request
```
insta.accept("username2accept",null);
insta.ignore("username2ignore",null);
```

#### Get followings / followers / counts
```
List<String> followers = insta.getFollowers("username",callback);
List<String> followings = insta.getFollowings("username",callback);
int followersCount = insta.getFollowersCount("username",null);
int followingCount = insta.getFollowingsCount("username",null);
int postCount = insta.getPostCount("username",callback);
```

#### Get profile metadata
```
String bio = insta.getBio("username",callback);
String url = insta.getProfilePicUrl("username",callback);
```
## Documentation

[Java docs](https://errorxcode.github.io/docs/easyinsta/index.html)


## FAQ

#### [Q.1] Can we use this library to make bots ?

Answer. No. Instagram don't allow to make bots with the use of this APIs.

#### [Q.2] Can we use this to download stories or posts ?

Answer. Yes. But its not currently supported.

#### [Q.3] Does use of this library requires any tokens or other things  ?

Answer. No. You only need to have username and password of the account.

#### [Q.4] Can we log in two-factor-authenticated accounts ?

Answer. Yes. Just pass a callback as thired argument while initializing the class.



## Contributing

Contributions are always welcome! Please make a pull request regarding any modification or feature request.


## Support

For support, follow us on [instagram](https://www.instagram.com/andro.developer).
 Also subscribe our [youtube](https://www.youtube.com/channel/UCcQS2F6LXAyuE_RXoIQxkMA) channel.
 It would be nice if you give this repo a star.

