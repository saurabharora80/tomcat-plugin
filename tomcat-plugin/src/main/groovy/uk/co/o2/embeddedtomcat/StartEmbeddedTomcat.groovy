package uk.co.o2.embeddedtomcat

import org.gradle.api.artifacts.Configuration

class StartEmbeddedTomcat {
    private Integer httpPort
    private String classpath
    private String tomcatbasedir
    private SslConfig ssl
    private Integer debugPort
    private String jvmOptions
    private String jvmProperties

    StartEmbeddedTomcat(File projectDir, Configuration gradleConfigurationForClasspath) {
        this(projectDir, gradleConfigurationForClasspath.collect { return it.absolutePath }.join(File.pathSeparator))
    }

    StartEmbeddedTomcat(File tomcatbasedir, String classpath) {
        this.classpath = classpath
        this.tomcatbasedir = tomcatbasedir
    }

    StartEmbeddedTomcat onHttpPort(Integer port) {
        this.httpPort = port
        return this
    }

    StartEmbeddedTomcat enableSSL(SslConfig ssl) {
        this.ssl = ssl
        return this
    }

    StartEmbeddedTomcat enableDebug(Integer port) {
        this.debugPort = port
        return this
    }

    StartEmbeddedTomcat withJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions
        return this
    }

    StartEmbeddedTomcat withJvmProperties(String[] jvmProperties) {
        this.jvmProperties = jvmProperties?.join(' ')
        return this
    }

    void andDeployApps(WarPath[] urlOfWarsToDeploy, File webappsDir) {
        String[] warnames = downloadOrCopyWarsOnCleanStartUp(urlOfWarsToDeploy, webappsDir)

        def processStartString = "java -classpath ${classpath} ${jvmArgs()} ${EmbeddedTomcat.class.name} ${httpPort} " +
                "$tomcatbasedir ${warnames.join(",")}"

        if (ssl != null) {
            processStartString += " $ssl.port $ssl.certLocation"
        }

        println("Starting Tomcat -> $processStartString")

        Process process = processStartString.execute()
        logOutput(process, tomcatbasedir)

        waitForTomcat()

    }

    private void waitForTomcat() {
        def tomcatPing = "http://localhost:${httpPort}/ping"
        println "Waiting for tomcat to start: ${tomcatPing}"
        def count = 0;
        while (count < 60) {
            try {
                Thread.sleep(1000)
                if (tomcatPing.toURL().text.equals("OK")) {
                    break
                }
            } catch (Exception exp) {
            }
            ++count
        }
        println "Tomcat Started"
    }

    private String[] downloadOrCopyWarsOnCleanStartUp(WarPath[] warPaths, File webappsDirectory) {
        if (!webappsDirectory.exists()) {
            webappsDirectory.mkdirs()
        }
        for (WarPath path : warPaths) {
            if (TomcatPlugin.shouldPerformCleanStartUp()) {
                if (path.isUrl()) {
                    println("-- Downloading $path.value")
                    new FileOutputStream("$webappsDirectory/$path.warname").write(path.value.openStream().bytes)
                } else {
                    println "-- coping $path.value to $webappsDirectory"
                    "cp $path.value $webappsDirectory".execute().waitFor()
                }
            } else {
                println " -- skipping downloading $path.warname; to download the war again pass -DcleanET"
            }
        }

        warPaths.collect { it.warname }
    }

    private void logOutput(Process process, String tomcatbasedir) {
        def tomcatLogStream = new File("$tomcatbasedir/tomcat.log").newOutputStream()
        process.consumeProcessOutput(tomcatLogStream, tomcatLogStream)
    }

    private String jvmArgs() {
        String jvmArgs = ''
        if (debugPort != null) {
            jvmArgs = "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${debugPort}"
        }
        if (jvmOptions != null) {
            jvmArgs += " $jvmOptions"
        }
        if (jvmProperties != null) {
            jvmArgs += " $jvmProperties"
        }
        jvmArgs
    }
}
