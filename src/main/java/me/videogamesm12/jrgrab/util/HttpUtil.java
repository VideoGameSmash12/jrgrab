package me.videogamesm12.jrgrab.util;

import lombok.Getter;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

public class HttpUtil
{
    @Getter
    private static final DateFormat dateFormat = new SimpleDateFormat("'['EEE',' dd MMM yyyy HH':'mm':'ss zzz']'");
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static String get(String url) throws IOException, InterruptedException
    {
        return httpClient.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    public static HttpResponse<String> getFull(String url) throws IOException, InterruptedException
    {
        return httpClient.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }

    public static String post(String url, String body) throws IOException, InterruptedException
    {
        return httpClient.send(HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    public static HttpResponse<String> uploadFile(String url, Path sourcePath, Map<String, String> headers)
			throws IOException, InterruptedException
	{
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofFile(sourcePath));
        headers.forEach(builder::header);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public static long downloadFile(String url, Path destination)
    {
		try
		{
			return Files.copy(new BufferedInputStream(new URL(url).openStream()), destination);
		}
		catch (IOException e)
		{
            return 0;
		}
	}
}
