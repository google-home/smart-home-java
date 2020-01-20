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

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

// With @WebServlet annotation the webapp/WEB-INF/web.xml is no longer required.
@WebServlet(name = "tokem", description = "Requests: Trivial request", urlPatterns = "/faketoken")
public class FakeTokenServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(MySmartHomeApp.class);
  private static int secondsInDay = 86400;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("text/plain");
    res.getWriter().println("/faketoken should be a POST");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String grantType = req.getParameter("grant_type");

    JsonObject jsonRes = new JsonObject();
    jsonRes.addProperty("token_type", "bearer");
    jsonRes.addProperty("access_token", "123access");
    jsonRes.addProperty("expires_in", secondsInDay);
    if (grantType.equals("authorization_code")) {
      jsonRes.addProperty("refresh_token", "123refresh");
    }
    res.setStatus(HttpServletResponse.SC_OK);
    res.setContentType("application/json");
    try {
      System.out.println("response = " + jsonRes.toString());
      res.getWriter().write(jsonRes.toString());
      res.getWriter().flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
