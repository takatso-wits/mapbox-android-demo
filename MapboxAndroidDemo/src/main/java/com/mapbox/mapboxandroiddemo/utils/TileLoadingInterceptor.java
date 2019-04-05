package com.mapbox.mapboxandroiddemo.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mapbox.mapboxsdk.Mapbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TileLoadingInterceptor implements Interceptor {

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    long elapsed = System.nanoTime();

    Response response = chain.proceed(request);
    elapsed = System.nanoTime() - elapsed;

    triggerPerformanceEvent(response, elapsed / 1000000);

    return response;
  }

  private void triggerPerformanceEvent(Response response, long elapsedMs) {
    List<Attribute<String>> attributes = new ArrayList<>();
    String request = getUrl(response.request());
    attributes.add(
            new Attribute<>("request_url", request));
    attributes.add(
            new Attribute<>("response_code", String.valueOf(response.code())));

    List<Attribute<Long>> counters = new ArrayList();
    counters.add(new Attribute<>("elapsed_ms", elapsedMs));

    JsonObject metaData = new JsonObject();
    metaData.addProperty("os", "android");
    metaData.addProperty("manufacturer", Build.MANUFACTURER);
    metaData.addProperty("brand", Build.BRAND);
    metaData.addProperty("device", Build.MODEL);
    metaData.addProperty("version", Build.VERSION.RELEASE);
    metaData.addProperty("abi", Build.CPU_ABI);
    metaData.addProperty("country", Locale.getDefault().getISO3Country());
    metaData.addProperty("ram", getRam());
    metaData.addProperty("screenSize", getWindowSize());

    Bundle bundle = new Bundle();
    Gson gson = new Gson();
    bundle.putString("attributes", gson.toJson(attributes));
    bundle.putString("counters", gson.toJson(counters));
    bundle.putString("metadata", metaData.toString());

    Mapbox.getTelemetry().onPerformanceEvent(bundle);

    Log.d(">>>",">>> PERF Event CODE=" + response.code()
            + " elapsed=" + elapsedMs + " url=" + request);
  }

  private String getUrl(Request request) {
    String url = request.url().toString();
    return url.substring(0, url.indexOf('?'));
  }

  private static String getRam() {
    ActivityManager actManager =
            (ActivityManager) Mapbox.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
    actManager.getMemoryInfo(memInfo);
    return String.valueOf(memInfo.totalMem);
  }

  private static String getWindowSize() {
    WindowManager windowManager =
            (WindowManager) Mapbox.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    DisplayMetrics metrics = new DisplayMetrics();
    display.getMetrics(metrics);
    int width = metrics.widthPixels;
    int height = metrics.heightPixels;

    return "{" + width + "," + height + "}";
  }

  private static class Attribute<T> {
    private String name;
    private T value;

    Attribute(String name, T value) {
      this.name = name;
      this.value = value;
    }
  }
}
