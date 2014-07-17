package uk.co.o2.embeddedtomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class StartTomcat {

    @SuppressWarnings("serial")
    public static void main(String[] args) throws ServletException, LifecycleException {

        final Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(args[1]);

        tomcat.setPort(Integer.parseInt(args[0]));

        for (String warPath : args[2].split(",")) {
            String contextPath = warPath.replaceAll(".*[\\/]" , "").replaceAll(".war","");
            tomcat.addWebapp("/" + contextPath, warPath);
            tomcat.getHost().setAppBase(contextPath);
        }

        Context context = tomcat.addContext("/", new File(".").getAbsolutePath());
        Tomcat.addServlet(context, "ping", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                PrintWriter writer = resp.getWriter();
                writer.write("OK");
                writer.flush();
            }
        });
        context.addServletMapping("/ping", "ping");
        Tomcat.addServlet(context, "shutdown", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                try {
                    PrintWriter writer = resp.getWriter();
                    writer.write("Stopping Tomcat...");
                    writer.flush();
                    tomcat.stop();
                } catch (LifecycleException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        context.addServletMapping("/shutdown", "shutdown");
        tomcat.start();
        tomcat.getServer().await();
    }

}
