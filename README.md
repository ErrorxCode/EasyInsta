
# EasyInsta
An library through which you can use instagram programatically. You can say that this is a well optimize, will featured java wrapper of Instagram private API. You can send direct messages, add stories, post photos, scrap profiles and can do many more things with this library.

Disclaimer ⚠: This API is private. Means that instagram has not documented or allowed others to use this API. If you are using this API then instagram can ban your account. **Developers will not be responsible for anything happend to your account**.

![Banner](https://i.ytimg.com/vi/jhTuFxpzevI/maxresdefault.jpg)

## Features

- Lightweight and Easy 2 use
- No need api token
- Supports **Sending messages** (Text & photos)
- Supports **Getting/fetching messages** (only Text)
- Supports **Deleting message**
- Supports **Spamming DMs**
- Supports **Login using proxy**
- Supports **Login using cache/without credentials**
- Supports **Posting** (Only photo)
- Supports **Adding stories** (Only photo)
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
	        implementation 'com.github.ErrorxCode:EasyInsta:2.5.0'
	}
```



## Acknowledgements

 - [Instagram graph api](https://developers.facebook.com/docs/instagram-api/)
 - [API Policies](https://developers.facebook.com/devpolicy/)


## Its easy :-)
```
Instagram.login("username","password").actions().doSomething().addOnCompleteListener(task -> {
    if (task.isSuccessful())
        System.out.println("Success");
    else 
        task.getException().printStackTrace();
});
```


## Documentation

[Java docs](https://errorxcode.github.io/docs/easyinsta/index.html)

[Guide](https://github.com/ErrorxCode/EasyInsta/wiki)


## FAQ

#### [Q.1] Can we use this library to make bots ?

Answer. Yes. But Instagram don't allow to make bots with their officail graph APIs. Altho This is not the officail api, but you should still follow the usage limits to prevent detection.

#### [Q.2] Can we download stories or posts using this API ?

Answer. No, Use [Instagram basic display API](https://developers.facebook.com/docs/instagram-basic-display-api/) for that.

#### [Q.3] Does use of this library requires any tokens or other keys ?

Answer. No. You only need to have username and password of the account.


## Contributing

Contributions are always welcome! Please make a pull request regarding any modification or feature request.


## Support

For support, follow us on [instagram](https://www.instagram.com/andro.developer).
Also subscribe our [youtube](https://www.youtube.com/channel/UCcQS2F6LXAyuE_RXoIQxkMA) channel.
It would be nice if you give this repo a star.

