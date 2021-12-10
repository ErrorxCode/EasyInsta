
# EasyInsta
An library through which you can use instagram programatically. You can say that this is a well optimize, will featured java wrapper of Instagram private API. You can send direct messages, add stories, post photos, scrap profiles and can do many more things with this library.

Disclaimer ⚠: This API is private. Means that instagram has not documented or allowed others to use this API. If you are using this API then instagram can ban your account. **Developer will not be responsible for anything happend to your account**.

![Banner](https://i.ytimg.com/vi/jhTuFxpzevI/maxresdefault.jpg)

## Features

- Lightweight and Easy 2 use
- No need api token
- Supports **sending direct message** (Text & photos)
- Supports **Login using proxy**
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
	        implementation 'com.github.ErrorxCode:EasyInsta:v2.0'
	}
```



## Acknowledgements

 - [Instagram graph api](https://developers.facebook.com/docs/instagram-api/)
 - [API Policies](https://developers.facebook.com/devpolicy/)


## API Reference

#### Create instagram object by logging in.
```java
try {
    Instagram insta = new Instagram("username","password");
} catch (IGLoginException e) {
    if (e.getReason() == Reasons.INVALID_CREDENTIALS){
        // Credential are incorrect
    } else if (e.getReason() == Reasons.REQUIRE_2_FACTOR_AUTHENTICATION){
        // You have to login using static method because Two-factor authentication is required.
    } else {
        // There might be other REASON.
        e.printStackTrace();
    }
}
```

#### Two factor login
```java
Instagram instagram = Instagram.login2factor("username", "password", new Callable<String>() {
    @Override
    public String call() throws Exception {
        // This method will wait until you call Instagram.verifyCode();
	// You have to return the verification code.
        return edittext.getText().toString();
    }
});

login.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Instagram.verifyCode();  // This should be called within 5 minutes of code sent.
    }
});
```

#### Task - The result

Each method returns a `Task` representing the response of the request. The resoponse either contain a value or an exception
depending upon success and failure. You can use the task in 2 ways. Ony way is,
```java
task.addOnCompleteListener(new Task.OnCompletionListener<String>() {
    @Override
    public void onComplete(Task<String> task) {
        String value;
        if (task.isSuccessful())
            String = task.getValue();
        else
            task.getException().printStackTrace();
    }
});
```
another way is,
```java
task.addOnSuccessListener(new Task.OnSuccessListener<String>() {
    @Override
    public void onSuccess(String value) {
        String followers = value;
    }
}).addOnFailureListener(new Task.OnFailureListener() {
    @Override
    public void onFailed(Throwable exception) {
        exception.printStackTrace();
    }
});
```

#### Now let's post something...
```java
Task<String> task = instagram.addStory(new File("story.png"));
Task<String task = instagram.postPhoto(new File("post.png"),"This is caption");
```

#### Send direct message
```java
Task<String> task = instagram.directMessage("x__coder__x","This is message...");
Task<String> task = instagram.directMessage("x__coder__x",new File("photo.jpg"));
```

#### Follow / Unfollow / remove someone
```java
instagram.follow("username2follow");
instagram.unfollow("username2unfollow");
instagram.removeFollower("username2remove");

```

#### Accept or ignore follow request
```java
instagram.accept("username2accept");
instagram.ignore("username2ignore");
```

#### Get followings / followers / counts
```java
Task<List<String>> followersTask = instagram.getFollowers("username");
Task<List<String>> followingTask = instagram.getFollowings("username");
Task<List<Integer>> followersCountTask = instagram.getFollowersCount("username");
Task<List<Integer>> followingsCountTask = instagram.getFollowingsCount("username");
Task<List<Integer>> postCountTask = instagram.getPostCount("username");
```

#### Get profile metadata
```java
instagram.getBio("username").addOnCompleteListener(new Task.OnCompletionListener<String>() {
            @Override
            public void onComplete(Task<String> task) {
                String bio;
                if (task.isSuccessful())
                    bio = task.getValue();
                else 
                    task.getException().printStackTrace();
            }
        });
	
instagram.getProfilePicUrl("username").addOnSuccessListener(new Task.OnSuccessListener<String>() {
            @Override
            public void onSuccess(String value) {
                String url = value;
            }
        }).addOnFailureListener(new Task.OnFailureListener() {
            @Override
            public void onFailed(Throwable exception) {
                exception.printStackTrace();
            }
        });
```
## Documentation

[Java docs](https://errorxcode.github.io/docs/easyinsta/index.html)


## FAQ

#### [Q.1] Can we use this library to make bots ?

Answer. No. Instagram don't allow to make bots with the use of APIs.

#### [Q.2] Can we download stories or posts using this API ?

Answer. No, Not currently. May be possible in future.

#### [Q.3] Does use of this library requires any tokens or other keys ?

Answer. No. You only need to have username and password of the account.


## Contributing

Contributions are always welcome! Please make a pull request regarding any modification or feature request.


## Support

For support, follow us on [instagram](https://www.instagram.com/andro.developer).
 Also subscribe our [youtube](https://www.youtube.com/channel/UCcQS2F6LXAyuE_RXoIQxkMA) channel.
 It would be nice if you give this repo a star.

