package uk.co.o2.embeddedtomcat

import org.gradle.api.Plugin
import org.gradle.api.Project

class TomcatPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("tomcatconfig", TomcatPluginExtension)
        TomcatPluginConfig config = new TomcatPluginConfig(project, "tomcatconfig")

        project.task("stopEmbeddedTomcat") << {
            EmbeddedTomcatStopper.stop(config.httpPort)
        }

        project.task('startEmbeddedTomcat').dependsOn('stopEmbeddedTomcat') << {
            if (shouldPerformCleanStartUp()) {
                println "-- clean startup; deleting embeddedtomcat directory"
                config.tomcatbasedir.deleteDir()
            } else {
                println "-- cleaning up logs, work and exploded war directories"
                new File("${config.tomcatbasedir}/logs").deleteDir()
                new File("${config.tomcatbasedir}/work").deleteDir()
                config.tomcatbasedir.listFiles().each { if (it.isFile()) { it.delete() } }
                config.webappdir.listFiles().each { if (it.isDirectory()) { it.deleteDir() }}
            }

            new StartEmbeddedTomcat(config.tomcatbasedir, project.configurations.embeddedtomcat)
                    .onHttpPort(config.httpPort).enableSSL(config.ssl).enableDebug(config.debugPort)
                    .withJvmOptions(config.jvmOptions).withJvmProperties(config.jvmProperties)
                    .andDeployApps(config.webappdir, config.urlOfWarsToDeploy)
        }
    }

    static boolean shouldPerformCleanStartUp() {
        System.getProperty("cleanET") != null
    }
}

class TomcatPluginExtension {
    def httpPort = 9191
    def warUrls
    def ssl
    def debugPort
    def jvmOptions = "-Xms256m -Xmx1G -XX:MaxPermSize=512m"
    def jvmProperties
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
        new File("$project.projectDir/embeddedtomcat")
    }


    File getWebappdir() {
        new File("$tomcatbasedir/webapps")
    }

    WarPath[] getUrlOfWarsToDeploy() {
        config().get("warUrls").collect { new WarPath((String) it) }
    }

    private Map config() {
        project.getExtensions().getByName(configwrappername).getProperties()
    }

    SslConfig getSsl() {
        def ssl = config().get("ssl")
        if (ssl == null) {
            return null;
        }
        new SslConfig(ssl)
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

class SslConfig {
    Integer port
    String certLocation
    TruststoreConfig truststore

    SslConfig(ssl) {
        this.port = Verify.isAnInteger(ssl.port, "ssl.port")
        this.certLocation = ssl.cert
        this.truststore = ssl.truststore ? new TruststoreConfig(path: ssl.truststore.path, password: ssl.truststore.password) : null
    }
}

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


