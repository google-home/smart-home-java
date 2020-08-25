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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.actions.api.smarthome.*;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.home.graph.v1.DeviceProto;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;

public class MySmartHomeApp extends SmartHomeApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(MySmartHomeApp.class);
  private static MyDataStore database = MyDataStore.getInstance();

  @NotNull
  @Override
  public SyncResponse onSync(SyncRequest syncRequest, Map<?, ?> headers) {

    SyncResponse res = new SyncResponse();
    res.setRequestId(syncRequest.requestId);
    res.setPayload(new SyncResponse.Payload());

    String token = (String) headers.get("authorization");
    String userId = "";
    try {
      userId = database.getUserId(token);
    } catch (Exception e) {
      // TODO(proppy): add errorCode when
      // https://github.com/actions-on-google/actions-on-google-java/issues/44 is fixed.
      LOGGER.error("failed to get user id for token: %d", token);
      return res;
    }
    res.payload.agentUserId = userId;

    database.setHomegraph(userId, true);
    List<QueryDocumentSnapshot> devices = new ArrayList<>();
    try {
      devices = database.getDevices(userId);
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error("failed to get devices", e);
      return res;
    }
    int numOfDevices = devices.size();
    res.payload.devices = new SyncResponse.Payload.Device[numOfDevices];
    for (int i = 0; i < numOfDevices; i++) {
      QueryDocumentSnapshot device = devices.get(i);
      SyncResponse.Payload.Device.Builder deviceBuilder =
          new SyncResponse.Payload.Device.Builder()
              .setId(device.getId())
              .setType((String) device.get("type"))
              .setTraits((List<String>) device.get("traits"))
              .setName(
                  DeviceProto.DeviceNames.newBuilder()
                      .addAllDefaultNames((List<String>) device.get("defaultNames"))
                      .setName((String) device.get("name"))
                      .addAllNicknames((List<String>) device.get("nicknames"))
                      .build())
              .setWillReportState((Boolean) device.get("willReportState"))
              .setRoomHint((String) device.get("roomHint"))
              .setDeviceInfo(
                  DeviceProto.DeviceInfo.newBuilder()
                      .setManufacturer((String) device.get("manufacturer"))
                      .setModel((String) device.get("model"))
                      .setHwVersion((String) device.get("hwVersion"))
                      .setSwVersion((String) device.get("swVersion"))
                      .build());
      if (device.contains("attributes")) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.putAll((Map<String, Object>) device.get("attributes"));
        String attributesJson = new Gson().toJson(attributes);
        Struct.Builder attributeBuilder = Struct.newBuilder();
        try {
          JsonFormat.parser().ignoringUnknownFields().merge(attributesJson, attributeBuilder);
        } catch (Exception e) {
          LOGGER.error("FAILED TO BUILD");
        }
        deviceBuilder.setAttributes(attributeBuilder.build());
      }
      if (device.contains("customData")) {
        Map<String, Object> customData = new HashMap<>();
        customData.putAll((Map<String, Object>) device.get("customData"));
        // TODO(proppy): remove once
        // https://github.com/actions-on-google/actions-on-google-java/issues/43 is fixed.
        String customDataJson = new Gson().toJson(customData);
        deviceBuilder.setCustomData(customDataJson);
      }
      if (device.contains("otherDeviceIds")) {
        deviceBuilder.setOtherDeviceIds((List) device.get("otherDeviceIds"));
      }
      res.payload.devices[i] = deviceBuilder.build();
    }

    return res;
  }

  @NotNull
  @Override
  public QueryResponse onQuery(QueryRequest queryRequest, Map<?, ?> headers) {
    QueryRequest.Inputs.Payload.Device[] devices =
        ((QueryRequest.Inputs) queryRequest.getInputs()[0]).payload.devices;
    QueryResponse res = new QueryResponse();
    res.setRequestId(queryRequest.requestId);
    res.setPayload(new QueryResponse.Payload());

    String token = (String) headers.get("authorization");
    String userId = "";
    try {
      userId = database.getUserId(token);
    } catch (Exception e) {
      LOGGER.error("failed to get user id for token: %d", headers.get("authorization"));
      res.payload.setErrorCode("authFailure");
      return res;
    }

    Map<String, Map<String, Object>> deviceStates = new HashMap<>();
    for (QueryRequest.Inputs.Payload.Device device : devices) {
      try {
        Map<String, Object> deviceState = database.getState(userId, device.id);
        deviceState.put("status", "SUCCESS");
        deviceStates.put(device.id, deviceState);
      } catch (Exception e) {
        LOGGER.error("QUERY FAILED: {}", e);
        Map<String, Object> failedDevice = new HashMap<>();
        failedDevice.put("status", "ERROR");
        failedDevice.put("errorCode", "deviceOffline");
        deviceStates.put(device.id, failedDevice);
      }
    }
    res.payload.setDevices(deviceStates);
    return res;
  }

  @NotNull
  @Override
  public ExecuteResponse onExecute(ExecuteRequest executeRequest, Map<?, ?> headers) {
    ExecuteResponse res = new ExecuteResponse();

    String token = (String) headers.get("authorization");
    String userId = "";
    try {
      userId = database.getUserId(token);
    } catch (Exception e) {
      LOGGER.error("failed to get user id for token: %d", headers.get("authorization"));
      res.setPayload(new ExecuteResponse.Payload());
      res.payload.setErrorCode("authFailure");
      return res;
    }

    List<ExecuteResponse.Payload.Commands> commandsResponse = new ArrayList<>();
    List<String> successfulDevices = new ArrayList<>();
    Map<String, Object> states = new HashMap<>();

    ExecuteRequest.Inputs.Payload.Commands[] commands =
        ((ExecuteRequest.Inputs) executeRequest.inputs[0]).payload.commands;
    for (ExecuteRequest.Inputs.Payload.Commands command : commands) {
      for (ExecuteRequest.Inputs.Payload.Commands.Devices device : command.devices) {
        try {
          states = database.execute(userId, device.id, command.execution[0]);
          successfulDevices.add(device.id);
          ReportState.makeRequest(this, userId, device.id, states);
        } catch (Exception e) {
          if (e.getMessage().equals("PENDING")) {
            ExecuteResponse.Payload.Commands pendingDevice = new ExecuteResponse.Payload.Commands();
            pendingDevice.ids = new String[] {device.id};
            pendingDevice.status = "PENDING";
            commandsResponse.add(pendingDevice);
            continue;
          }
          if (e.getMessage().equals("pinNeeded")) {
            ExecuteResponse.Payload.Commands failedDevice = new ExecuteResponse.Payload.Commands();
            failedDevice.ids = new String[] {device.id};
            failedDevice.status = "ERROR";
            failedDevice.setErrorCode("challengeNeeded");
            failedDevice.setChallengeNeeded(
                new HashMap<String, String>() {
                  {
                    put("type", "pinNeeded");
                  }
                });
            failedDevice.setErrorCode(e.getMessage());
            commandsResponse.add(failedDevice);
            continue;
          }
          if (e.getMessage().equals("challengeFailedPinNeeded")) {
            ExecuteResponse.Payload.Commands failedDevice = new ExecuteResponse.Payload.Commands();
            failedDevice.ids = new String[] {device.id};
            failedDevice.status = "ERROR";
            failedDevice.setErrorCode("challengeNeeded");
            failedDevice.setChallengeNeeded(
                new HashMap<String, String>() {
                  {
                    put("type", "challengeFailedPinNeeded");
                  }
                });
            failedDevice.setErrorCode(e.getMessage());
            commandsResponse.add(failedDevice);
            continue;
          }
          if (e.getMessage().equals("ackNeeded")) {
            ExecuteResponse.Payload.Commands failedDevice = new ExecuteResponse.Payload.Commands();
            failedDevice.ids = new String[] {device.id};
            failedDevice.status = "ERROR";
            failedDevice.setErrorCode("challengeNeeded");
            failedDevice.setChallengeNeeded(
                new HashMap<String, String>() {
                  {
                    put("type", "ackNeeded");
                  }
                });
            failedDevice.setErrorCode(e.getMessage());
            commandsResponse.add(failedDevice);
            continue;
          }

          ExecuteResponse.Payload.Commands failedDevice = new ExecuteResponse.Payload.Commands();
          failedDevice.ids = new String[] {device.id};
          failedDevice.status = "ERROR";
          failedDevice.setErrorCode(e.getMessage());
          commandsResponse.add(failedDevice);
        }
      }
    }

    ExecuteResponse.Payload.Commands successfulCommands = new ExecuteResponse.Payload.Commands();
    successfulCommands.status = "SUCCESS";
    successfulCommands.setStates(states);
    successfulCommands.ids = successfulDevices.toArray(new String[] {});
    commandsResponse.add(successfulCommands);

    res.requestId = executeRequest.requestId;
    ExecuteResponse.Payload payload =
        new ExecuteResponse.Payload(
            commandsResponse.toArray(new ExecuteResponse.Payload.Commands[] {}));
    res.setPayload(payload);

    return res;
  }

  @NotNull
  @Override
  public void onDisconnect(DisconnectRequest disconnectRequest, Map<?, ?> headers) {
    String token = (String) headers.get("authorization");
    try {
      String userId = database.getUserId(token);
      database.setHomegraph(userId, false);
    } catch (Exception e) {
      LOGGER.error("failed to get user id for token: %d", token);
    }
  }
}
