package uk.co.o2.gradleplugin.multiwartomcat

import groovy.transform.ToString
import org.gradle.api.Plugin
import org.gradle.api.Project

class TomcatPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("multiwartomcatconfig", TomcatPluginExtension)
        TomcatPluginConfig config = new TomcatPluginConfig(project, "multiwartomcatconfig")

        project.task("stopMultiwarTomcat") << {
            MultiwarTomcatStopper.stop(config.httpPort)
        }

        project.task('startMultiwarTomcat').dependsOn('stopMultiwarTomcat') << {
            new StartMultiwarTomcat(config.tomcatbasedir, project.configurations.multiwartomcat)
                    .onHttpPort(config.httpPort).enableSSL(config.ssl).enableDebug(config.debugPort)
                    .withJvmOptions(config.jvmOptions).withJvmProperties(config.jvmProperties)
                    .andDeployApps(config.webappdir, config.urlOfWarsToDeploy)
        }
    }
}

@ToString
class TomcatPluginExtension {
    def httpPort = 8080
    def wars
    def ssl
    def debugPort
    def jvmOptions = "-Xms256m -Xmx1G -XX:MaxPermSize=512m"
    def jvmProperties
    def enableSsl = false
}

class TomcatPluginConfig {
    private Project project
    private String configwrappername

    TomcatPluginConfig(Project project, String configwrapper) {
        this.configwrappername = configwrapper
        this.project = project
    }

    Integer getHttpPort() {
        Verify.isAnInteger(config(), "httpPort");
    }


    File getTomcatbasedir() {
        new File("$project.projectDir/multiwartomcat")
    }


    File getWebappdir() {
        new File("$tomcatbasedir/webapps")
    }

    WarPath[] getUrlOfWarsToDeploy() {
        config().get("wars").collect { new WarPath((String) it) }
    }

    private Map config() {
        project.getExtensions().getByName(configwrappername).getProperties()
    }

    SslConfig getSsl() {
        if(config().get("enableSsl")) {
            return new SslConfig(config().get("ssl"), tomcatbasedir)
        }
        return null
    }

    Integer getDebugPort() {
        Verify.isAnInteger(config(), 'debugPort')
    }

    String getJvmOptions() {
        config().get('jvmOptions')
    }

    String[] getJvmProperties() {
        config().get('jvmProperties')
    }
}

@ToString
class SslConfig {
    Integer port
    String cert
    TruststoreConfig truststore
    boolean defaultSelfSignedCert
    boolean defaultTrustore

    SslConfig(def ssl, File tomcatbasedir) {
        if (ssl && ssl.port) {
            port = Verify.isAnInteger(ssl.port, "ssl.port")
        } else {
            port = 8443
        }
        if (ssl && ssl.cert) {
            cert = ssl.cert
        } else {
            defaultSelfSignedCert = true
            cert = readFromClasspath(tomcatbasedir, "selfsignedcert.keystore")
        }
        if (ssl && ssl.truststore) {
            truststore = new TruststoreConfig(path: ssl.truststore.path, password: ssl.truststore.password)
        } else {
            defaultTrustore = true
            truststore = new TruststoreConfig(path: readFromClasspath(tomcatbasedir, "apigatewaycert-truststore.jks"), password: "password")
        }
    }

    private String readFromClasspath(File tomcatbasedir, String filename) {
        File sslDir = new File("$tomcatbasedir/ssl")
        if(!sslDir.exists()) {
            sslDir.mkdirs()
        }

        File certFile = new File(sslDir, filename)
        certFile.write(this.class.getClassLoader().getResourceAsStream("ssl/$filename").text)

        certFile.absolutePath
    }
}

@ToString
class TruststoreConfig {
    String path
    String password
}

class Verify {
    public static Integer isAnInteger(Map config, String key) {
        isAnInteger(config.get(key), key)
    }

    public static Integer isAnInteger(Object get, String key) {
        try {
            (Integer) get
        } catch (ClassCastException ex) {
            throw new RuntimeException("'$key' must be an Integer")
        }
    }
}


