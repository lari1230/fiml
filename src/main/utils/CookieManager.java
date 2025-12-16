package main.utils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CookieManager {
    public static String getCookie(HttpExchange exchange, String name) {
        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("Cookie")) {
            String cookies = headers.getFirst("Cookie");
            Map<String, String> cookieMap = parseCookies(cookies);
            return cookieMap.get(name);
        }
        return null;
    }

    public static void setCookie(HttpExchange exchange, String name, String value, int maxAge) {
        Headers headers = exchange.getResponseHeaders();
        String cookie = String.format("%s=%s; Path=/; HttpOnly; Max-Age=%d",
                name, value, maxAge);
        headers.add("Set-Cookie", cookie);
    }

    public static void removeCookie(HttpExchange exchange, String name) {
        Headers headers = exchange.getResponseHeaders();
        String cookie = String.format("%s=; Path=/; HttpOnly; Max-Age=0", name);
        headers.add("Set-Cookie", cookie);
    }

    private static Map<String, String> parseCookies(String cookieString) {
        return Arrays.stream(cookieString.split(";"))
                .map(String::trim)
                .map(cookie -> cookie.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts[1]
                ));
    }
}