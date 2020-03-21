/*
 * Copyright 2020 Google LLC
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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.actions.api.smarthome.SmartHomeApp;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.home.graph.v1.HomeGraphApiServiceProto;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

/**
 * A singleton class to encapsulate state reporting behavior with changing ColorSetting state
 * values.
 */
final class ReportState {
  private static final Logger LOGGER = LoggerFactory.getLogger(MySmartHomeApp.class);

  private ReportState() {}

  /**
   * Creates and completes a ReportStateAndNotification request
   *
   * @param actionsApp The SmartHomeApp instance to use to make the gRPC request
   * @param userId The agent user ID
   * @param deviceId The device ID
   * @param states A Map of state keys and their values for the provided device ID
   */
  public static void makeRequest(
      SmartHomeApp actionsApp, String userId, String deviceId, Map<String, Object> states) {
    // Convert a Map of states to a JsonObject
    JsonObject jsonStates = (JsonObject) JsonParser.parseString(new Gson().toJson(states));
    ReportState.makeRequest(actionsApp, userId, deviceId, jsonStates);
  }

  /**
   * Creates and completes a ReportStateAndNotification request
   *
   * @param actionsApp The SmartHomeApp instance to use to make the gRPC request
   * @param userId The agent user ID
   * @param deviceId The device ID
   * @param states A JSON object of state keys and their values for the provided device ID
   */
  public static void makeRequest(
      SmartHomeApp actionsApp, String userId, String deviceId, JsonObject states) {
    // Do state name replacement for ColorSetting trait
    // See https://developers.google.com/assistant/smarthome/traits/colorsetting#device-states
    JsonObject colorJson = states.getAsJsonObject("color");
    if (colorJson != null && colorJson.has("spectrumRgb")) {
      colorJson.add("spectrumRGB", colorJson.get("spectrumRgb"));
      colorJson.remove("spectrumRgb");
    }
    Struct.Builder statesStruct = Struct.newBuilder();
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(new Gson().toJson(states), statesStruct);
    } catch (Exception e) {
      LOGGER.error("FAILED TO BUILD");
    }

    HomeGraphApiServiceProto.ReportStateAndNotificationDevice.Builder deviceBuilder =
        HomeGraphApiServiceProto.ReportStateAndNotificationDevice.newBuilder()
            .setStates(
                Struct.newBuilder()
                    .putFields(deviceId, Value.newBuilder().setStructValue(statesStruct).build()));

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
}
