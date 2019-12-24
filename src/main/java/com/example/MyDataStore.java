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

import com.google.actions.api.smarthome.ExecuteRequest;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyDataStore {

    private static final String DATABASE_URL = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(MySmartHomeApp.class);
    private static MyDataStore ourInstance = new MyDataStore();
    private static Firestore database;

    public MyDataStore() {
        // Use a service account
        try {
            InputStream serviceAccount = new FileInputStream("WEB-INF/smart-home-key.json");

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(credentials)
                    .setDatabaseUrl(DATABASE_URL).build();
            FirebaseApp.initializeApp(options);

            database = FirestoreClient.getFirestore();

        } catch (Exception e) {
            LOGGER.error("ERROR: invalid service account credentials. See README.");
            LOGGER.error(e.getMessage());

            System.exit(1);
        }
    }

    public static MyDataStore getInstance() {
        return ourInstance;
    }

    public List<QueryDocumentSnapshot> getDevices(String userId) {
        List<QueryDocumentSnapshot> devices = new ArrayList<>();
        try {
            ApiFuture<QuerySnapshot> deviceQuery =
                    database.collection("users").document(userId)
                            .collection("devices").get();
            QuerySnapshot querySnapshot = deviceQuery.get();
            devices = querySnapshot.getDocuments();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return devices;
    }

    public String getUserId(String token) throws ExecutionException, InterruptedException {
        if (token == null) {
            token = "Bearer 123access";
        }
        ApiFuture<QuerySnapshot> userQuery =
                database.collection("users").whereEqualTo("fakeAccessToken",
                        token.substring(7))
                        .get();
        QuerySnapshot usersSnapshot = userQuery.get();
        List<QueryDocumentSnapshot> users = usersSnapshot.getDocuments();

        DocumentSnapshot user;
        try {
            user = users.get(0);
        } catch (Exception e) {
            LOGGER.error("no user found!");
            throw e;
        }

        return user.getId();
    }

    public Boolean isHomegraphEnabled(String userId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot user = database.collection("users").document(userId).get().get();
        return (Boolean) user.get("homegraph");
    }

    public void setHomegraph(String userId, Boolean enable) {
        DocumentReference user = database.collection("users").document(userId);
        user.update("homegraph", enable);
    }

    public void updateDevice(String userId, String deviceId,
                             Map<String, Object> states,
                             Map<String, String> params) {
        DocumentReference device =
                database.collection("users").document(userId)
                        .collection("devices")
                        .document(deviceId);
        if (states != null) {
            device.update("states", states);
        }
        if (params.containsKey("deviceName")) {
          String name = params.get("deviceName");
          device.update("name", name != null ? name : FieldValue.delete());
        }
        if (params.containsKey("deviceNickname")) {
          String nickname = params.get("nickname");
          device.update("nickname", nickname != null ? nickname : FieldValue.delete());
        }
        if (params.containsKey("errorCode")) {
          String errorCode = params.get("errorCode");
          device.update("errorCode", errorCode != null ? errorCode : FieldValue.delete());
        }
        if (params.containsKey("tfa")) {
          String tfa = params.get("tfa");
          device.update("tfa", tfa != null ? tfa : FieldValue.delete());
        }
        if (params.containsKey("localDeviceId")) {
            String localDeviceId = params.get("localDeviceId");
            if (localDeviceId != null) {
                Map<String, Object> otherDeviceId = new HashMap<>();
                otherDeviceId.put("deviceId", localDeviceId);
                List<Object> otherDeviceIds = new ArrayList<>();
                otherDeviceIds.add(otherDeviceId);
                device.update("otherDeviceIds", otherDeviceIds);
            } else {
                device.update("otherDeviceIds", FieldValue.delete());
            }
        }
    }

    public void addDevice(String userId, Map<String, Object> data) {
        String deviceId = (String) data.get("deviceId");
        database.collection("users").document(userId).collection("devices")
                .document(deviceId)
                .set(data);
    }

    public void deleteDevice(String userId, String deviceId) {
        database.collection("users").document(userId).collection("devices")
                .document(deviceId)
                .delete();
    }

    public Map<String, Object> getState(String userId, String deviceId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot device =
                database.collection("users").document(userId)
                        .collection("devices")
                        .document(deviceId).get().get();
        return (Map<String, Object>) device.get("states");
    }

    public Map<String, Object> execute(String userId, String deviceId,
            ExecuteRequest.Inputs.Payload.Commands.Execution execution)
            throws Exception {

        DocumentSnapshot device =
                database.collection("users").document(userId)
                        .collection("devices")
                        .document(deviceId).get().get();
        Map<String, Object> deviceStates = (Map<String, Object>) device.getData().get("states");
        Map<String, Object> states = new HashMap<>();
        if (device.contains("states")) {
            states.putAll(deviceStates);
        }

        if (!(Boolean) states.get("online")) {
            throw new Exception("deviceOffline");
        }

        if (!device.getString("errorCode").isEmpty()) {
            throw new Exception(device.getString("errorCode"));
        }

        if (device.getString("tfa").equals("ack") && execution.getChallenge() == null) {
            throw new Exception("ackNeeded");
        } else if (!device.getString("tfa").isEmpty() && execution.getChallenge() == null) {
            throw new Exception("pinNeeded");
        } else if (!device.getString("tfa").isEmpty() && execution.getChallenge() != null) {
          String pin = (String) execution.getChallenge().get("pin");
          if (pin != null && !pin.equals(device.getString("tfa"))) {
            throw new Exception("challengeFailedPinNeeded");
          }
        }

        switch (execution.command) {
        // action.devices.traits.ArmDisarm
        case "action.devices.commands.ArmDisarm":
            if (execution.getParams().containsKey("arm")) {
                boolean isArmed = (boolean) execution.getParams().get("arm");
                states.put("isArmed", isArmed);
            } else if (execution.getParams().containsKey("cancel")) {
                // Cancel value is in relation to the arm value
                boolean isArmed = (boolean) execution.getParams().get("arm");
                states.put("isArmed", !isArmed);
            }
            if (execution.getParams().containsKey("armLevel")) {
                database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.isArmed", states.get("isArmed"),
                        "states.currentArmLevel", execution.getParams().get("armLevel"));
                    states.put("currentArmLevel", execution.getParams().get("armLevel"));
            } else {
                database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("isArmed", states.get("isArmed"));
            }
            break;

        // action.devices.traits.Brightness
        case "action.devices.commands.BrightnessAbsolute":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.brightness", execution.getParams()
                            .get("brightness"));
            states.put("brightness", execution.getParams().get("brightness"));
            break;

        // action.devices.traits.CameraStream
        case "action.devices.commands.GetCameraStream":
            states.put("cameraStreamAccessUrl", "https://fluffysheep.com/baaaaa.mp4");
            break;

        // action.devices.traits.ColorSetting
        case "action.devices.commands.ColorAbsolute":
            String colorType;
            Object color;
            Map<String, Object> colorMap =
                    (Map<String, Object>) execution.getParams().get("color");

            if (colorMap.containsKey("spectrumRGB")) {
                database.collection("users").document(userId)
                        .collection("devices")
                        .document(deviceId)
                        .update("states.color.spectrumRgb", colorMap.get("spectrumRGB"));
                color = colorMap.get("spectrumRGB");
                colorType = "spectrumRgb";
            } else {
                if (colorMap.containsKey("spectrumHSV")) {
                    database.collection("users").document(userId)
                            .collection("devices")
                            .document(deviceId)
                            .update("states.color.spectrumHsv", colorMap.get("spectrumHSV"));
                    colorType = "spectrumHsv";
                    color = colorMap.get("spectrumHSV");

                } else {
                    if (colorMap.containsKey("temperature")) {
                        database.collection("users").document(userId)
                                .collection("devices")
                                .document(deviceId)
                                .update("states.color.temperatureK",
                                        colorMap.get("temperature"));
                        colorType = "temperatureK";
                        color = colorMap.get("temperature");

                    } else {
                        throw new Exception("notSupported");
                    }
                }
            }
            states.put(colorType, color);
            break;

        // action.devices.traits.Cook
        case "action.devices.commands.Cook":
            boolean startCooking = (boolean) execution.getParams().get("start");
            if (startCooking) {
                // Start cooking
                Map<String, Object> dbStates = new HashMap<String, Object>() {{
                    put("states.currentCookingMode", execution.getParams().get("cookingMode"));
                }};
                if (execution.getParams().containsKey("foodPreset")) {
                    dbStates.put("states.currentFoodPreset", execution.getParams().get("foodPreset"));
                } else {
                    dbStates.put("states.currentFoodPreset", "NONE");
                }
                if (execution.getParams().containsKey("quantity")) {
                    dbStates.put("states.currentFoodQuantity", execution.getParams().get("quantity"));
                } else {
                    dbStates.put("states.currentFoodQuantity", 0);
                }
                if (execution.getParams().containsKey("unit")) {
                    dbStates.put("states.currentFoodUnit", execution.getParams().get("unit"));
                } else {
                    dbStates.put("states.currentFoodUnit", "NONE");
                }
                database.collection("users").document(userId)
                    .collection("devices")
                    .document("deviceId")
                    .update(dbStates);
                // Server getting response will handle any undefined values
                states.put("currentCookingMode", execution.getParams().get("cookingMode"));
                states.put("currentFoodPreset", execution.getParams().get("foodPreset"));
                states.put("currentFoodQuantity", execution.getParams().get("quantity"));
                states.put("currentFoodUnit", execution.getParams().get("unit"));
            } else {
                // Done cooking, reset
                database.collection("users").document(userId)
                    .collection("devices")
                    .document("deviceId")
                    .update(new HashMap<String, Object>() {{
                        put("states.currentCookingMode", "NONE");
                        put("states.currentFoodPreset", "NONE");
                        put("states.currentFoodQuantity", 0);
                        put("states.currentFoodUnit", "NONE");
                    }});
                states.put("currentCookingMode", "NONE");
                states.put("currentFoodPreset", "NONE");
            }
            break;

        // action.devices.traits.Dispense
        case "action.devices.commands.Dispense":
          int amount = (int) execution.getParams().get("amount");
          String unit = (String) execution.getParams().get("unit");
          if (execution.getParams().containsKey("presetName") &&
              execution.getParams().get("presetName").equals("cat food bowl")) {
            // Fill in params
            amount = 4;
            unit = "CUPS";
          }
          Map<String, Object> amountLastDispensed = new HashMap();
          amountLastDispensed.put("amount", amount);
          amountLastDispensed.put("unit", unit);
          Map<String, Object> dispenseUpdates = new HashMap<>();
          dispenseUpdates.put("states.dispenseItems", new HashMap[] {
              new HashMap<String, Object>() {{
                  put("itemName", execution.getParams().get("item"));
                  put("amountLastDispensed", amountLastDispensed);
                  put("isCurrentlyDispensing", execution.getParams().containsKey("presetName"));
              }}
          });
          database.collection("users").document(userId)
              .collection("devices")
              .document(deviceId)
              .update(dispenseUpdates);
          states.put("dispenseItems", new HashMap[] {
              new HashMap<String, Object>() {{
                put("itemName", execution.getParams().get("item"));
                put("amountLastDispensed", amountLastDispensed);
                put("isCurrentlyDispensing", execution.getParams().containsKey("presetName"));
              }}
          });
        break;

        // action.devices.traits.Dock
        case "action.devices.commands.Dock":
            // This has no parameters
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.isDocked", true);
            states.put("isDocked", true);
            break;

        // action.devices.traits.FanSpeed
        case "action.devices.commands.SetFanSpeed":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.currentFanSpeedSetting",
                    execution.getParams().get("fanSpeed"));
            states.put("currentFanSpeedSetting", execution.getParams().get("fanSpeed"));
            break;

        case "action.devices.commands.Reverse":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.currentFanSpeedReverse",
                    true);
            break;

        // action.devices.traits.Fill
        case "action.devices.commands.Fill":
            Map<String, Object> updates = new HashMap<>();
            String currentFillLevel = "none";
            boolean fill = (boolean) execution.getParams().get("fill");
            if (fill) {
                if (execution.getParams().containsKey("fillLevel")) {
                    currentFillLevel = (String) execution.getParams().get("fillLevel");
                } else {
                    currentFillLevel = "half"; // Default fill level
                }
            } // Else the device is draining and the fill level is set to "none" by default
            updates.put("states.isFilled", fill);
            updates.put("states.currentFillLevel", currentFillLevel);
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update(updates);
            states.put("isFilled", fill);
            states.put("currentFillLevel", currentFillLevel);
            break;

        // action.devices.traits.HumiditySetting
        case "action.devices.commands.SetHumidity":
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.humiditySetpointPercent", execution.getParams().get("humiditySetpointPercent"));
            states.put("humiditySetpointPercent", execution.getParams().get("humiditySetpointPercent"));
            break;

        // action.devices.traits.Locator
        case "action.devices.commands.Locate":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.silent", execution.getParams().get("silent"),
                            "states.generatedAlert", true);
            states.put("generatedAlert", true);
            break;

        // action.devices.traits.LockUnlock
        case "action.devices.commands.LockUnlock":
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.isLocked", execution.getParams().get("lock"));
            states.put("isLocked", execution.getParams().get("lock"));
            break;

        // action.devices.traits.OnOff
        case "action.devices.commands.OnOff":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.on",
                    execution.getParams().get("on"));
            states.put("on", execution.getParams().get("on"));
            break;


        // action.devices.traits.OpenClose
        case "action.devices.commands.OpenClose":
            // Check if the device can open in multiple directions
            Map<String, Object> attributes = (Map<String, Object>)device.getData().get("attributes");
            if (attributes != null && attributes.containsKey("openDirection")) {
                // The device can open in more than one direction
                String direction = (String) execution.getParams().get("openDirection");
                List<Map<String, Object>> openStates = (List<Map<String, Object>>) states.get("openState");
                openStates.forEach(state -> {
                    if (state.get("openDirection").equals(direction)) {
                        state.put("openPercent", execution.getParams().get("openPercent"));
                    }
                });
                states.put("openStates", openStates);
                database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.openState", openStates);
            } else {
                // The device can only open in one direction
                database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.openPercent", execution.getParams().get("openPercent"));
                states.put("openPercent", execution.getParams().get("openPercent"));
            }
            break;

        // action.devices.traits.Reboot
        case "action.devices.commands.Reboot":
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.online", false);
            break;

        // action.devices.traits.Rotation
        case "action.devices.commands.RotateAbsolute":
            // Check if the device can open in multiple directions
            if (execution.getParams().containsKey("rotationPercent")) {
                database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.rotationPercent", execution.getParams().get("rotationPercent"));
                states.put("rotationPercent", execution.getParams().get("rotationPercent"));
            } else if (execution.getParams().containsKey("rotationDegrees")) {
                database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.rotationDegrees", execution.getParams().get("rotationDegrees"));
                states.put("rotationDegrees", execution.getParams().get("rotationDegrees"));
            }
            break;

        // action.devices.traits.RunCycle - No execution
        // action.devices.traits.Scene
        case "action.devices.commands.ActivateScene":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.deactivate", execution.getParams()
                            .get("deactivate"));
            // Scenes are stateless
            break;

        // action.devices.traits.SoftwareUpdate
        case "action.devices.commands.SoftwareUpdate":
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update(new HashMap<String, Object>() {{
                    put("states.online", false);
                    put("states.lastSoftwareUpdateUnixTimestampSec", new Date().getTime() / 1000);
                }});
            break;

        // action.devices.traits.StartStop
        case "action.devices.commands.StartStop":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.isRunning", execution.getParams().get("start"));
            states.put("isRunning", execution.getParams().get("start"));
            break;

        case "action.devices.commands.PauseUnpause":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.isPaused", execution.getParams().get("pause"));
            states.put("isPaused", execution.getParams().get("pause"));
            break;

        // action.devices.traits.Modes
        case "action.devices.commands.SetModes":
            Map<String, Object> currentModeSettings = (Map<String, Object>) states
                    .getOrDefault("currentModeSettings", new HashMap<String, Object>());
            currentModeSettings.putAll((Map<String, Object>) execution.getParams()
                    .getOrDefault("updateModeSettings", new HashMap<String, Object>()));
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.currentModeSettings", currentModeSettings);
            states.put("currentModeSettings", currentModeSettings);
            break;

        // action.devices.traits.Timer
        case "action.devices.commands.TimerStart":
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.timerRemainingSec", execution.getParams().get("timerTimeSec"));
            states.put("timerRemainingSec", execution.getParams().get("timerTimeSec"));
            break;

        case "action.devices.commands.TimerAdjust":
            if ((int) states.get("timerRemainingSec") == -1) {
                // No timer exists
                throw new RuntimeException("noTimerExists");
            }
            int newTimerRemainingSec = (int) states.get("timerRemainingSec") +
                (int) execution.getParams().get("timerTimeSec");
            if (newTimerRemainingSec < 0) {
                throw new RuntimeException("valueOutOfRange");
            }
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.timerRemainingSec", newTimerRemainingSec);
            states.put("timerRemainingSec", newTimerRemainingSec);
            break;

        case "action.devices.commands.TimerPause":
            if ((int) states.get("timerRemainingSec") == -1) {
                // No timer exists
                throw new RuntimeException("noTimerExists");
            }
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.timerPaused", true);
            states.put("timerPaused", true);
            break;

        case "action.devices.commands.TimerResume":
            if ((int) states.get("timerRemainingSec") == -1) {
                // No timer exists
                throw new RuntimeException("noTimerExists");
            }
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.timerPaused", false);
            states.put("timerPaused", false);
            break;

        case "action.devices.commands.TimerCancel":
            if ((int) states.get("timerRemainingSec") == -1) {
                // No timer exists
                throw new RuntimeException("noTimerExists");
            }
            database.collection("users").document(userId)
                .collection("devices")
                .document(deviceId)
                .update("states.timerRemainingSec", -1);
            states.put("timerRemainingSec", 0);
            break;

        // action.devices.traits.Toggles
        case "action.devices.commands.SetToggles":
            Map<String, Object> currentToggleSettings = (Map<String, Object>) states
                    .getOrDefault("currentToggleSettings", new HashMap<String, Object>());
            currentToggleSettings.putAll((Map<String, Object>) execution.getParams()
                    .getOrDefault("updateToggleSettings", new HashMap<String, Object>()));
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.currentToggleSettings", currentToggleSettings);
            states.put("currentToggleSettings", currentToggleSettings);
            break;


        // action.devices.traits.TemperatureControl
        case "action.devices.commands.SetTemperature":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.temperatureSetpointCelsius",
                    execution.getParams().get("temperature"));
            states.put("temperatureSetpointCelsius", execution.getParams().get("temperature"));
            states.put("temperatureAmbientCelsius",
                    deviceStates.get("temperatureAmbientCelsius"));
            break;

        // action.devices.traits.TemperatureSetting
        case "action.devices.commands.ThermostatTemperatureSetpoint":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.thermostatTemperatureSetpoint",
                    execution.getParams().get("thermostatTemperatureSetpoint"));
            states.put("thermostatTemperatureSetpoint",
                    execution.getParams().get("thermostatTemperatureSetpoint"));
            states.put("thermostatMode", deviceStates.get("states.thermostatMode"));
            states.put("thermostatTemperatureAmbient",
                    deviceStates.get("thermostatTemperatureAmbient"));
            states.put("thermostatHumidityAmbient",
                    deviceStates.get("thermostatHumidityAmbient"));
            break;

        case "action.devices.commands.ThermostatTemperatureSetRange":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.thermostatTemperatureSetpointLow",
                    execution.getParams().get("thermostatTemperatureSetpointLow"),
                    "states.thermostatTemperatureSetpointHigh",
                    execution.getParams().get("thermostatTemperatureSetpointHigh"));
            states.put("thermostatTemperatureSetpoint",
                    deviceStates.get("thermostatTemperatureSetpoint"));
            states.put("thermostatMode", deviceStates.get("thermostatMode"));
            states.put("thermostatTemperatureAmbient",
                    deviceStates.get("thermostatTemperatureAmbient"));
            states.put("thermostatHumidityAmbient",
                    deviceStates.get("thermostatHumidityAmbient"));
            break;

        case "action.devices.commands.ThermostatSetMode":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.thermostatMode",
                    execution.getParams().get("thermostatMode"));
            states.put("thermostatMode", execution.getParams().get("thermostatMode"));
            states.put("thermostatTemperatureSetpoint",
                    deviceStates.get("thermostatTemperatureSetpoint"));
            states.put("thermostatTemperatureAmbient",
                    deviceStates.get("thermostatTemperatureAmbient"));
            states.put("thermostatHumidityAmbient",
                    deviceStates.get("thermostatHumidityAmbient"));
            break;

        }

        return states;

    }


}
