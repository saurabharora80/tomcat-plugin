package uk.co.o2.embeddedtomcat

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class TomcatPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("tomcatconfig", TomcatPluginExtension)

        project.task('runWars') << {
            TomcatPluginConfig config = new TomcatPluginConfig(project, "tomcatconfig")
            new File(config.getTomcatbasedir()).deleteDir()

            new StartEmbeddedTomcat(project.projectDir, project.configurations.embeddedtomcat)
                    .onPort(config.httpPort)
                    .andDeployApps(config.urlOfWarsToDeploy)
        }
    }
}

class TomcatPluginExtension {
    def httpPort = "9191"
    def warUrls
}

class TomcatPluginConfig {
    private Project project
    private String configwrappername

    TomcatPluginConfig(Project project, String configwrapper) {
        this.configwrappername = configwrapper
        this.project = project
    }

    String getHttpPort() {
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

}


