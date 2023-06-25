package com.xcoder.easyinsta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import port.org.json.JSONObject;

public class HiddenAPI {

    protected static List<String> login(String username, String password, boolean isDesktop) throws IOException {
        password = "#PWD_INSTAGRAM_BROWSER:0:" + new Date().getTime() + ":" + password;
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        var body = new FormBody.Builder()
                .add("enc_password",password)
                .add("username",username)
                .add("queryParams","{}")
                .build();
        Request request = new Request.Builder()
                .url("https://www.instagram.com/api/v1/web/accounts/login/ajax/")
                .method("POST", body)
                .addHeader("authority", "www.instagram.com")
                .addHeader("accept", "*/*")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("x-csrftoken",isDesktop ? getCrsf() : "")
                .addHeader("user-agent", isDesktop ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36" : "Instagram 219.0.0.12.117 Android")
                .build();
        Response response = client.newCall(request).execute();
        return response.headers("set-cookie");
    }

    protected static String getCrsf() throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("text/plain");
        Request request = new Request.Builder()
                .url("https://www.instagram.com/api/v1/web/login_page/")
                .addHeader("authority", "www.instagram.com")
                .addHeader("accept", "application/json")
                .get()
                .build();
        Response response = client.newCall(request).execute();
        return response.headers("Set-Cookie").get(0).split(";")[0].split("=")[1];
    }

    protected static String getCookieFromHeaders(List<String> headers){
        String cookie = "";
        for (String header : headers) {
            var crsf = header.split(";")[0];
            cookie += crsf + ";";
        }
        return cookie.replaceFirst(".$","");
    }
}
