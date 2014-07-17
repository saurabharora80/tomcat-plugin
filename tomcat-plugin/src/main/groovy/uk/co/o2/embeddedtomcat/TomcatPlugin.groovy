package uk.co.o2.embeddedtomcat

import org.gradle.api.Plugin
import org.gradle.api.Project

class TomcatPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("tomcatconfig", TomcatPluginExtension)

        project.task('runWars') << {
            TomcatPluginConfig config = new TomcatPluginConfig(project, "tomcatconfig")
            def processStartString = "java -classpath ${classpath(project)} uk.co.o2.embeddedtomcat.StartTomcat ${config.httpPort} " +
                    "$config.tomcatbasedir $config.war"

            println("executing $processStartString")

            Process process = processStartString.execute()
            process.waitFor()

            PrintStream tomcatLogStream = new PrintStream(new File("$config.tomcatbasedir/tomcat.log").absolutePath)
            process.consumeProcessOutput(tomcatLogStream, tomcatLogStream)
        }
    }

    private String classpath(Project project) {
        project.configurations.embeddedtomcat.collect { return it.absolutePath }.join(File.pathSeparator)
    }

}

class TomcatPluginExtension {
    def httpPort = "9191"
    def wars
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
        "$project.projectDir/build"
    }

    String getWar() {
        config().get("wars")?.join(',');
    }

    private Map config() {
        project.getExtensions().getByName(configwrappername).getProperties()
    }
}