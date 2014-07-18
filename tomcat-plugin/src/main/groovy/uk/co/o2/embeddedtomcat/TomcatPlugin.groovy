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
                    .onHttpPort(config.httpPort).enableSSL(config.ssl)
                    .andDeployApps(config.urlOfWarsToDeploy)
        }
    }
}

class TomcatPluginExtension {
    def httpPort = 9191
    def warUrls
    def ssl
}

class TomcatPluginConfig {
    private Project project
    private String configwrappername

    TomcatPluginConfig(Project project, String configwrapper) {
        this.configwrappername = configwrapper
        this.project = project
    }

    Integer getHttpPort() {
        config().get("httpPort");
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
        if(ssl == null) {
            return null;
        }
        new SslConfig(port: ssl.port, certLocation: "$project.projectDir/${ssl.cert}")
    }
}

class SslConfig {
    Integer port
    String certLocation
}


