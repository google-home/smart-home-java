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

import com.google.actions.api.smarthome.SmartHomeApp;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.home.graph.v1.HomeGraphApiServiceProto;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

    {
        try {
            InputStream serviceAccount = new FileInputStream("WEB-INF/smart-home-key.json");
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            actionsApp.setCredentials(credentials);
        } catch (Exception e) {
            LOGGER.error("couldn't load credentials");
        }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String body = req.getReader().lines().collect(Collectors.joining());
        JSONObject bodyJson = new JSONObject(body);
        String userId = bodyJson.getString("userId");
        String deviceId = bodyJson.getString("deviceId");
        LOGGER.info("doPost, body = {}", body);
        String deviceName = bodyJson.has("name") ? bodyJson.getString("name") : null;
        String deviceNickname = bodyJson.has("nickname") ? bodyJson.getString("nickname")
                : null;
        Map<String, Object> deviceStates = bodyJson.has("states") ?
                new Gson().fromJson(bodyJson.getJSONObject("states").toString(), HashMap.class) :
                null;
        String errorCode = bodyJson.has("errorCode") ? bodyJson.getString("errorCode") : null;
        String tfa = bodyJson.has("tfa") ? bodyJson.getString("tfa") : null;
        try {
            database.updateDevice(userId, deviceId, deviceName, deviceNickname, deviceStates, errorCode, tfa);
            if (deviceStates != null) {
                JSONObject statesJson = new JSONObject(deviceStates);
                // Do state name replacement for ColorSetting trait
                // See https://developers.google.com/assistant/smarthome/traits/colorsetting#device-states
                if (statesJson.has("color") &&
                        statesJson.getJSONObject("color").has("spectrumRgb")) {
                    statesJson.getJSONObject("color")
                        .put("spectrumRGB", statesJson.getJSONObject("color").get("spectrumRgb"));
                    statesJson.getJSONObject("color").remove("spectrumRgb");
                }
                Struct.Builder statesStruct = Struct.newBuilder();
                try {
                    JsonFormat.parser().ignoringUnknownFields()
                            .merge(statesJson.toString(), statesStruct);
                } catch (Exception e) {
                    LOGGER.error("FAILED TO BUILD");
                }

                HomeGraphApiServiceProto.ReportStateAndNotificationDevice.Builder deviceBuilder =
                        HomeGraphApiServiceProto.ReportStateAndNotificationDevice.newBuilder()
                                .setStates(Struct.newBuilder().putFields(deviceId,
                                        Value.newBuilder().setStructValue(statesStruct).build()));

                HomeGraphApiServiceProto.ReportStateAndNotificationRequest request =
                        HomeGraphApiServiceProto.ReportStateAndNotificationRequest.newBuilder()
                                .setRequestId(String.valueOf(Math.random()))
                                .setAgentUserId("1836.15267389") // our single user's id
                                .setPayload(HomeGraphApiServiceProto.
                                        StateAndNotificationPayload.newBuilder()
                                        .setDevices(deviceBuilder)).build();

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

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/plain");
        response.getWriter().println("/smarthome/update is a POST call");
    }


    @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
        // pre-flight request processing
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        res.setHeader("Access-Control-Allow-Headers",
                "X-Requested-With,Content-Type,Accept,Origin");

    }
}
