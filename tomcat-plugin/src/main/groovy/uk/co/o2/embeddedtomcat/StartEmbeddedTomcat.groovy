package uk.co.o2.embeddedtomcat

import org.gradle.api.artifacts.Configuration

class StartEmbeddedTomcat {
    private Integer httpPort
    private String classpath
    private String tomcatbasedir
    private SslConfig ssl

    StartEmbeddedTomcat(File projectDir, Configuration gradleConfigurationForClasspath) {
        this(projectDir, gradleConfigurationForClasspath.collect { return it.absolutePath }.join(File.pathSeparator))
    }

    StartEmbeddedTomcat(File projectDir, String classpath) {
        this.classpath = classpath
        this.tomcatbasedir = "$projectDir/embeddedtomcat"
    }

    StartEmbeddedTomcat onHttpPort(Integer port) {
        this.httpPort = port
        return this
    }

    StartEmbeddedTomcat enableSSL(SslConfig ssl) {
        this.ssl = ssl
        return this
    }

    void andDeployApps(WarUrl[] urlOfWarsToDeploy) {
        String[] warnames = downloadWars(urlOfWarsToDeploy, "$tomcatbasedir/webapps")

        def processStartString = "java -classpath ${classpath} uk.co.o2.embeddedtomcat.StartTomcat ${httpPort} " +
                "$tomcatbasedir ${warnames.join(",")}"

        if(ssl!=null) {
            processStartString += " $ssl.port $ssl.certLocation"
        }

        println("Starting Tomcat -> $processStartString")

        Process process = processStartString.execute()
        process.waitFor()

        logOutput(process, tomcatbasedir)
    }

    private String[] downloadWars(WarUrl[] urlOfWarsToDeploy, String appbase) {
        File webappsDirectory = new File(appbase)
        if(!webappsDirectory.exists()) {
            webappsDirectory.mkdirs()
        }
        for (WarUrl url : urlOfWarsToDeploy) {
            println("Downloading ${url.get()}")
            new FileOutputStream("$appbase/$url.warname").write(url.get().openStream().bytes)
        }
        urlOfWarsToDeploy.collect { it.warname }
    }

    private void logOutput(Process process, String tomcatbasedir) {
        def tomcatLogStream = new File("$tomcatbasedir/tomcat.log").newOutputStream()
        process.waitForProcessOutput(tomcatLogStream, tomcatLogStream)
    }
}
