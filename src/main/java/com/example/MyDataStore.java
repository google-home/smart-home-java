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
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

    public void updateDevice(String userId, String deviceId, String deviceName,
            String deviceNickname, Map<String, Object> states) {
        DocumentReference device =
                database.collection("users").document(userId)
                        .collection("devices")
                        .document(deviceId);
        if (deviceName != null) {
            device.update("name", deviceName);
        }
        if (deviceNickname != null) {
            device.update("nickname", deviceNickname);
        }
        if (states != null) {
            device.update("states", states);
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


        switch (execution.command) {
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

        // action.devices.traits.Locator
        case "action.devices.commands.Locate":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId)
                    .update("states.silent", execution.getParams().get("silent"),
                            "states.generatedAlert", true);
            states.put("generatedAlert", true);
            break;

        // action.devices.traits.OnOff
        case "action.devices.commands.OnOff":
            database.collection("users").document(userId)
                    .collection("devices")
                    .document(deviceId).update("states.on",
                    execution.getParams().get("on"));
            states.put("on", execution.getParams().get("on"));
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
