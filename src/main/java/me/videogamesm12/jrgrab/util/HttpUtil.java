package me.videogamesm12.jrgrab.util;

import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class HttpUtil
{
    @Getter
    private static final DateFormat dateFormat = new SimpleDateFormat("'['EEE',' dd MMM yyyy HH':'mm':'ss zzz']'");
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Sends a GET request to a given resource and returns the resulting body
     * @param url                   A URL to a given resource
     * @return                      The body of the request's response.
     * @throws IOException          If some IO error occurs while attempting to make the request
     * @throws InterruptedException If the process is interrupted while it's trying to make the request
     */
    public static String get(String url) throws IOException, InterruptedException
    {
        return httpClient.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    /**
     * Sends a GET request to a given resource and returns the response
     * @param url                   A URL to a given resource
     * @return                      The request's response
     * @throws IOException          If some IO error occurs while attempting to make the request
     * @throws InterruptedException If the process is interrupted while it's trying to make the request
     */
    public static HttpResponse<String> getFull(String url) throws IOException, InterruptedException
    {
        return httpClient.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a POST request to a given resource and returns the resulting body
     * @param url                   A URL to a given resource
     * @return                      The body of the request's response.
     * @throws IOException          If some IO error occurs while attempting to make the request
     * @throws InterruptedException If the process is interrupted while it's trying to make the request
     */
    public static String post(String url, String body) throws IOException, InterruptedException
    {
        return httpClient.send(HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    /**
     * Sends a HEAD request to a given resource and returns the response
     * @param url                   A URL to a given resource
     * @return                      The request's response
     * @throws IOException          If some IO error occurs while attempting to make the request
     * @throws InterruptedException If the process is interrupted while it's trying to make the request
     */
    public static HttpResponse<Void> head(String url) throws IOException, InterruptedException
	{
        return httpClient.send(HttpRequest.newBuilder(URI.create(url)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
    }
}
