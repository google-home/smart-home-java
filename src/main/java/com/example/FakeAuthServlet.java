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
import java.net.URLDecoder;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// With @WebServlet annotation the webapp/WEB-INF/web.xml is no longer required.
@WebServlet(name = "auth", description = "Requests: Trivial request", urlPatterns = "/fakeauth")
public class FakeAuthServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String newUrl =
        res.encodeRedirectURL(
            String.format(
                "%s?code=%s&state=%s",
                URLDecoder.decode(req.getParameter("redirect_uri"), "UTF8"),
                "xxxxxx",
                req.getParameter("state")));
    res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    res.setHeader("Location", newUrl);
    res.getWriter().flush();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("text/plain");
    res.getWriter().println("/fakeauth should be a GET");
  }
}
