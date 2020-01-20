/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.actions.api.smarthome.SmartHomeApp;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.home.graph.v1.HomeGraphApiServiceProto;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

/**
 * Handles request received via HTTP POST and delegates it to your Actions app. See: [Request
 * handling in Google App
 * Engine](https://cloud.google.com/appengine/docs/standard/java/how-requests-are-handled).
 */
@WebServlet(name = "smarthomeUpdate", urlPatterns = "/smarthome/update")
public class SmartHomeUpdateServlet extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(MySmartHomeApp.class);
  private static MyDataStore database = MyDataStore.getInstance();
  private final SmartHomeApp actionsApp = new MySmartHomeApp();
  private static final List<String> UPDATE_DEVICE_PARAMS_KEYS =
      Arrays.asList(new String[] {"name", "nickname", "localDeviceId", "errorCode", "tfa"});

  {
    try {
      InputStream serviceAccount = new FileInputStream("WEB-INF/smart-home-key.json");
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      actionsApp.setCredentials(credentials);
    } catch (Exception e) {
      LOGGER.error("couldn't load credentials");
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String body = req.getReader().lines().collect(Collectors.joining());
    LOGGER.info("doPost, body = {}", body);
    JsonObject bodyJson = new JsonParser().parse(body).getAsJsonObject();
    String userId = bodyJson.get("userId").getAsString();
    String deviceId = bodyJson.get("deviceId").getAsString();
    JsonObject deviceStatesJson = bodyJson.getAsJsonObject("states");
    Map<String, Object> deviceStates =
        deviceStatesJson != null ? new Gson().fromJson(deviceStatesJson, HashMap.class) : null;
    Map<String, String> deviceParams = new HashMap<>();
    Set<String> deviceParamsKeys = bodyJson.keySet();
    deviceParamsKeys.retainAll(UPDATE_DEVICE_PARAMS_KEYS);
    for (String k : deviceParamsKeys) {
      deviceParams.put(k, bodyJson.get(k).getAsString());
    }
    try {
      database.updateDevice(userId, deviceId, deviceStates, deviceParams);
      if (deviceParams.containsKey("localDeviceId")) {
        actionsApp.requestSync(userId);
      }
      if (deviceStatesJson != null) {
        // Do state name replacement for ColorSetting trait
        // See https://developers.google.com/assistant/smarthome/traits/colorsetting#device-states
        JsonObject colorJson = deviceStatesJson.getAsJsonObject("color");
        if (colorJson != null && colorJson.has("spectrumRgb")) {
          colorJson.add("spectrumRGB", colorJson.get("spectrumRgb"));
          colorJson.remove("spectrumRgb");
        }
        Struct.Builder statesStruct = Struct.newBuilder();
        try {
          JsonFormat.parser()
              .ignoringUnknownFields()
              .merge(new Gson().toJson(deviceStatesJson), statesStruct);
        } catch (Exception e) {
          LOGGER.error("FAILED TO BUILD");
        }

        HomeGraphApiServiceProto.ReportStateAndNotificationDevice.Builder deviceBuilder =
            HomeGraphApiServiceProto.ReportStateAndNotificationDevice.newBuilder()
                .setStates(
                    Struct.newBuilder()
                        .putFields(
                            deviceId, Value.newBuilder().setStructValue(statesStruct).build()));

        HomeGraphApiServiceProto.ReportStateAndNotificationRequest request =
            HomeGraphApiServiceProto.ReportStateAndNotificationRequest.newBuilder()
                .setRequestId(String.valueOf(Math.random()))
                .setAgentUserId(userId) // our single user's id
                .setPayload(
                    HomeGraphApiServiceProto.StateAndNotificationPayload.newBuilder()
                        .setDevices(deviceBuilder))
                .build();

        actionsApp.reportState(request);
      }
    } catch (Exception e) {
      LOGGER.error("failed to update device");
      throw e;
    } finally {
      res.setHeader("Access-Control-Allow-Origin", "*");
      res.setContentType("text/plain");
      res.getWriter().println("OK");
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("text/plain");
    response.getWriter().println("/smarthome/update is a POST call");
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
    // pre-flight request processing
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "X-Requested-With,Content-Type,Accept,Origin");
  }
}
