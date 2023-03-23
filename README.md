
# EasyInsta
<p align="left">
  <a href="#"><img alt="Languages-Java" src="https://img.shields.io/badge/Language-Java-1DA1F2?style=flat-square&logo=java"></a>
  <a href="#"><img alt="Version" src="https://jitpack.io/v/ErrorxCode/EasyInsta.svg"></a>
  <a href="https://www.instagram.com/x0.rahil/"><img alt="Instagram - x__coder__" src="https://img.shields.io/badge/Instagram-x0.rahil-lightgrey"></a>
  <a href="#"><img alt="Downloads" src="https://jitpack.io/v/ErrorxCode/EasyInsta/month.svg"></a>
  <a href="#"><img alt="GitHub Repo stars" src="https://img.shields.io/github/stars/ErrorxCode/EasyInsta?style=social"></a>
  </p>
An library through which you can use instagram programatically. You can say that this is a well optimize, will featured wrapper of instagram4j library. You can send direct messages, add stories, post photos, scrap profiles and can do many more things with this library.

Disclaimer ⚠: This API is private. Means that instagram has not documented or allowed others to use this API. If you are using this API **harshly** then instagram may ban your account. **Developers will not be responsible for anything happend to your account**.

![Banner](https://i.ytimg.com/vi/jhTuFxpzevI/maxresdefault.jpg)

## Features

- Lightweight and Easy 2 use
- No need api token
- Supports **Sending messages**
- Supports **Getting/fetching messages**
- Supports **Deleting message**
- Supports **Spamming DMs**
- Supports **_Realtime direct messages listener_**
- Supports **Login using cache/saving sessions**
- Supports **Posting**
- Supports **Adding stories**
- Supports **Following / Unfollowing others**
- Supports **Acception / Ignoring follow request**
- Supports **Scrapping followings and followers**
- Supports **Getting profile data**
- Supports **Liking/commenting on post**
- Supports **Fetching feeds/timeline post**
- 

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
	        implementation 'com.github.ErrorxCode:EasyInsta:2.8'
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
Contributions are always welcome! 

There is always a scope of improvement in this library. What you can do is you can add more endpoints from [instagram4j](https://github.com/instagram4j/instagram4j) library.


## Support

For support, follow us on [instagram](https://www.instagram.com/x0.rahil).
It would be nice if you give this repo a star.

