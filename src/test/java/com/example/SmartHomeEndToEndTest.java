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

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.cloud.firestore.QueryDocumentSnapshot;

class SmarHomeEndToEndTest {
  private static final String USER_ID = "test-user-id";
  private static final String DEVICE_ID = "test-device-id";
  private static final String DEVICE_NAME = "test-device-name";
  private static final String DEVICE_NAME_UPDATED = "test-device-name-updated";
  private static final String DEVICE_TYPE = "action.devices.types.LIGHT";
  private static final String DEVICE_PLACEHOLDER = "test-device-placeholder";
  private static final String REQUEST_ID = "request-id";

  @BeforeAll()
  static void initAll() throws ExecutionException, InterruptedException {
    Map<String, Object> testUser = new HashMap<>();
    if (System.getProperty("restassuredBaseUri") != null) {
      io.restassured.RestAssured.baseURI = System.getProperty("restassuredBaseUri");
    }
    testUser.put("fakeAccessToken", "123access");
    testUser.put("fakeRefreshToken", "123refresh");
    MyDataStore.getInstance().database.collection("users").document(USER_ID).set(testUser).get();
  }

  @AfterAll()
  static void tearDownAll() throws ExecutionException, InterruptedException {
    MyDataStore.getInstance().database.collection("users").document(USER_ID).delete().get();
  }

  @Test
  void testCreateSyncUpdateDelete() throws ExecutionException, InterruptedException {
    Map<String, Object> deviceCreate = new HashMap<>();
    deviceCreate.put("userId", USER_ID);
    Map<String, Object> deviceData = new HashMap<>();
    deviceData.put("deviceId", DEVICE_ID);
    deviceData.put("name", DEVICE_NAME);
    deviceData.put("type", DEVICE_TYPE);
    deviceData.put("traits", new ArrayList<String>());
    deviceData.put("defaultNames", new ArrayList<String>());
    deviceData.put("nicknames", new ArrayList<String>());
    deviceData.put("willReportState", false);
    deviceData.put("roomHint", DEVICE_PLACEHOLDER);
    deviceData.put("manufacturer", DEVICE_PLACEHOLDER);
    deviceData.put("model", DEVICE_PLACEHOLDER);
    deviceData.put("hwVersion", DEVICE_PLACEHOLDER);
    deviceData.put("swVersion", DEVICE_PLACEHOLDER);
    deviceCreate.put("data", deviceData);
    given()
        .contentType("application/json")
        .body(deviceCreate)
        .when()
        .post("/smarthome/create")
        .then()
        .statusCode(200);

    QueryDocumentSnapshot deviceCreated = MyDataStore.getInstance().getDevices(USER_ID).get(0);
    assertEquals(DEVICE_ID, deviceCreated.get("deviceId"));

    Map<String, Object> syncRequest = new HashMap<>();
    syncRequest.put("requestId", REQUEST_ID);
    List<Map<String, Object>> syncRequestInputs = new ArrayList<>();
    syncRequest.put("inputs", syncRequestInputs);
    Map<String, Object> syncRequestIntent = new HashMap<>();
    syncRequestIntent.put("intent", "action.devices.SYNC");
    syncRequestInputs.add(syncRequestIntent);
    given()
        .contentType("application/json")
        .body(syncRequest)
        .when()
        .post("/smarthome")
        .then()
        .statusCode(200)
        .body(
            "requestId", equalTo(REQUEST_ID),
            "payload.devices[0].id", equalTo(DEVICE_ID),
            "payload.devices[0].name.name", equalTo(DEVICE_NAME),
            "payload.devices[0].type", equalTo(DEVICE_TYPE));

    Map<String, Object> deviceUpdate = new HashMap<>();
    deviceUpdate.put("userId", USER_ID);
    deviceUpdate.put("deviceId", DEVICE_ID);
    deviceUpdate.put("name", DEVICE_NAME_UPDATED);
    deviceUpdate.put("type", DEVICE_TYPE);
    given()
        .contentType("application/json")
        .body(deviceUpdate)
        .when()
        .post("/smarthome/update")
        .then()
        .statusCode(200);

    QueryDocumentSnapshot deviceUpdated = MyDataStore.getInstance().getDevices(USER_ID).get(0);
    assertEquals(DEVICE_NAME_UPDATED, deviceUpdated.get("name"));

    Map<String, Object> deviceDelete = new HashMap<>();
    deviceDelete.put("userId", USER_ID);
    deviceDelete.put("deviceId", DEVICE_ID);
    given()
        .contentType("application/json")
        .body(deviceDelete)
        .when()
        .post("/smarthome/delete")
        .then()
        .statusCode(200);

    assertEquals(0, MyDataStore.getInstance().getDevices(USER_ID).size());
  }
}
