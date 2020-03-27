package com.example;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "login", description = "Trivial login page", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String redirectURL = req.getParameter("responseurl");
    res.setStatus(HttpServletResponse.SC_OK);
    res.setContentType("text/html");
    String formData =
        "<html>"
            + "<body>"
            + "<form action='/login' method='post'>"
            + "<input type='hidden' name='responseurl' value='"
            + redirectURL
            + "'/>"
            + "<button type='submit' style='font-size:14pt'>Link this service to Google</button>"
            + "</form>"
            + "</body>"
            + "</html>";
    res.getWriter().print(formData);
    res.getWriter().flush();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    // Here, you should validate the user account.
    // In this sample, we do not do that.
    String redirectURL = URLDecoder.decode(req.getParameter("responseurl"), "UTF-8");
    res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    res.setHeader("Location", redirectURL);
    res.getWriter().flush();
  }
}
