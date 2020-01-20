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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.actions.api.smarthome.SmartHomeApp;
import com.google.auth.oauth2.GoogleCredentials;

/**
 * Handles request received via HTTP POST and delegates it to your Actions app. See: [Request
 * handling in Google App
 * Engine](https://cloud.google.com/appengine/docs/standard/java/how-requests-are-handled).
 */
@WebServlet(name = "smarthome", urlPatterns = "/smarthome")
public class SmartHomeServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(MySmartHomeApp.class);
  private final SmartHomeApp actionsApp = new MySmartHomeApp();

  {
    try {
      InputStream serviceAccount = new FileInputStream("WEB-INF/smart-home-key.json");
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      actionsApp.setCredentials(credentials);
    } catch (Exception e) {
      LOG.error("couldn't load credentials");
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String body = req.getReader().lines().collect(Collectors.joining());
    LOG.info("doPost, body = {}", body);
    Map<String, String> headerMap = getHeaderMap(req);
    try {
      String response = actionsApp.handleRequest(body, headerMap).get();
      writeResponse(res, response);
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("text/plain");
    response
        .getWriter()
        .println(
            "ActionsServlet is listening but requires valid POST "
                + "request to respond with Action response.");
  }

  private void writeResponse(HttpServletResponse res, String asJson) {
    try {
      System.out.println("response = " + asJson);
      res.getWriter().write(asJson);
      res.getWriter().flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Map<String, String> getHeaderMap(HttpServletRequest req) {
    Map<String, String> headerMap = new HashMap<>();
    Enumeration headerNames = req.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = (String) headerNames.nextElement();
      String val = req.getHeader(name);
      headerMap.put(name, val);
    }
    return headerMap;
  }
}
