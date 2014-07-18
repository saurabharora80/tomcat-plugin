package uk.co.o2.embeddedtomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
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

        int port = Integer.parseInt(args[0]);
        String tomcatBasedir = args[1];
        String warnames = args[2];

        final Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(tomcatBasedir);
        tomcat.getHost().setAppBase("webapps");

        tomcat.setPort(port);

        if (args.length > 4) {
            int httpsPort = Integer.parseInt(args[3]);
            String keystore = args[4];
            tomcat.getService().addConnector(new HttpsConnector(httpsPort, keystore));
        }

        for (String warname : warnames.split(",")) {
            String contextPath = contextPath(warname);
            tomcat.addWebapp("/" + contextPath, warname);
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

    private static String contextPath(String warPath) {
        return warPath.replace(".war", "");
    }

}

class HttpsConnector extends Connector {
    HttpsConnector(int httpsPort, String keystore) {
        setScheme("https");
        setPort(httpsPort);
        setProperty("maxPostSize", "0");  // unlimited
        setProperty("xpoweredBy", "true");
        setSecure(true);
        setProperty("SSLEnabled", "true");
        setProperty("clientAuth", "false");
        setProperty("sslProtocol", "TLS");
        setProperty("keystoreFile", keystore);
        setProperty("keystorePass", "changeit");
    }
}
