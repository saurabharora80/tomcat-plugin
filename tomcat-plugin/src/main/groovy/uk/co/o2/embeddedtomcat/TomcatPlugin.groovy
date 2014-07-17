package uk.co.o2.embeddedtomcat

import org.gradle.api.Plugin
import org.gradle.api.Project

class TomcatPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("tomcatconfig", TomcatPluginExtension)

        project.task('runWars') << {
            TomcatPluginConfig config = new TomcatPluginConfig(project, "tomcatconfig")
            new File(config.getAppbase()).mkdirs()
            new StartEmbeddedTomcat(config, classpath(project)).onPort(config.httpPort).andDeployApps(downloadWars(config))
        }
    }

    private String[] downloadWars(TomcatPluginConfig config) {
        for (WarUrl url : config.getUrlOfWarsToDeploy()) {
            new FileOutputStream("$config.appbase/$url.warname").write(url.get().openStream().bytes)
        }
        config.getUrlOfWarsToDeploy().collect { it.warname }
    }

    private String classpath(Project project) {
        project.configurations.embeddedtomcat.collect { return it.absolutePath }.join(File.pathSeparator)
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

    String getAppbase() {
        "$tomcatbasedir/webapps"
    }
}

class WarUrl {
    private String url

    WarUrl(String url) {
        this.url = url
    }

    URL get() {
        url.toURL()
    }

    String getWarname() {
        url.split("/").last()
    }
}

class StartEmbeddedTomcat {
    private TomcatPluginConfig config
    private String port
    private String classpath

    StartEmbeddedTomcat(TomcatPluginConfig config, String classpath) {
        this.classpath = classpath
        this.config = config
    }

    StartEmbeddedTomcat onPort(String port) {
        this.port = port
        return this
    }

    void andDeployApps(String[] warnames) {
        def processStartString = "java -classpath ${classpath} uk.co.o2.embeddedtomcat.StartTomcat ${port} " +
                "$config.tomcatbasedir ${warnames.join(",")}"

        println("executing $processStartString")

        Process process = processStartString.execute()
        process.waitFor()

        logOutput(process, config.tomcatbasedir)
    }

    private void logOutput(Process process, String tomcatbasedir) {
        def tomcatLogStream = new File("$tomcatbasedir/tomcat.log").newOutputStream()
        process.waitForProcessOutput(tomcatLogStream, tomcatLogStream)
    }
}