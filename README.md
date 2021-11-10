
# EasyInsta
This is an android library through which you can use instagram programatically.
You can say that this is a well optimize, will featured java wrapper of "Instagram graph API".
You can direct messages, add stories, post photos, scrapping profiles and can do many more things with this library.

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
	        implementation 'com.github.ErrorxCode:EasyInsta:v1'
	}
```



## Acknowledgements

 - [Instagram graph api](https://developers.facebook.com/docs/instagram-api/)
 - [API Policies](https://developers.facebook.com/devpolicy/)


## Documentation

[Java docs]()


## API Reference

#### Logs in to instagram account
```
