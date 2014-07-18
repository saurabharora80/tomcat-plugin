package uk.co.o2.embeddedtomcat

import org.gradle.api.Plugin
import org.gradle.api.Project

class TomcatPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("tomcatconfig", TomcatPluginExtension)

        project.task('runWars') << {
            TomcatPluginConfig config = new TomcatPluginConfig(project, "tomcatconfig")
            new File(config.getTomcatbasedir()).deleteDir()

            new StartEmbeddedTomcat(project.projectDir, project.configurations.embeddedtomcat)
                    .onHttpPort(config.httpPort).enableSSL(config.ssl).enableDebug(config.debugPort)
                    .withJvmOptions(config.jvmOptions).withJvmProperties(config.jvmProperties)
                    .andDeployApps(config.urlOfWarsToDeploy)
        }
    }
}

class TomcatPluginExtension {
    def httpPort = 9191
    def warUrls
    def ssl
    def debugPort
    def jvmOptions
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


    String getTomcatbasedir() {
        "$project.projectDir/embeddedtomcat"
    }

    WarUrl[] getUrlOfWarsToDeploy() {
        config().get("warUrls").collect { new WarUrl((String) it) }
    }

    private Map config() {
        project.getExtensions().getByName(configwrappername).getProperties()
    }

    SslConfig getSsl() {
        def ssl = config().get("ssl")
        if (ssl == null) {
            return null;
        }
        new SslConfig(port: Verify.isAnInteger(ssl.port, "ssl.port"), certLocation: "$project.projectDir/${ssl.cert}")
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


