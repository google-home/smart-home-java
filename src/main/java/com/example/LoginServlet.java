package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "login", description = "Trivial login page", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(MySmartHomeApp.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    LOGGER.error("====== resquest : " + req.toString());

    String redirectURL = req.getParameter("responseurl") + "&code=xxxxxx";
    res.setStatus(HttpServletResponse.SC_OK);
    res.setContentType("text/html");
    LOGGER.error("====== responseurl : " + redirectURL);
    String formData =
        "<html>"
            + "<body>"
            + "<br>"
            + "<br>"
            + "<form action='/login' method='post'>"
            + "<input type='hidden' name='responseurl' value='"
            + redirectURL
            + "'/>"
            + "<button type='submit' style='font-size:14pt'>Link this service to Google Murilo</button>"
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
