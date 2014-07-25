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
    private File webappdir

    StartEmbeddedTomcat(File projectDir, Configuration gradleConfigurationForClasspath) {
        this(projectDir, gradleConfigurationForClasspath.collect { return it.absolutePath }.join(File.pathSeparator))
    }

    StartEmbeddedTomcat(File tomcatbasedir, String classpath) {
        this.classpath = classpath
        this.tomcatbasedir = tomcatbasedir
        this.webappdir = new File("$tomcatbasedir/webapps")

        if (shouldPerformCleanStartUp()) {
            if (tomcatbasedir.exists()) {
                println "-- clean startup; deleting embeddedtomcat directory"
                tomcatbasedir.deleteDir()
            }
        } else {
            println "-- cleaning up logs, work and exploded war directories"
            new File("${tomcatbasedir}/logs").deleteDir()
            new File("${tomcatbasedir}/work").deleteDir()
            tomcatbasedir.listFiles().each { if (it.isFile()) {it.delete()}}
            this.webappdir.listFiles().each { if (it.isDirectory()) { it.deleteDir() }}
        }
    }

    private static boolean shouldPerformCleanStartUp() {
        System.getProperty("cleanET") != null
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

    void andDeployApps(File webappsDir, WarPath... urlOfWarsToDeploy) {
        String[] warnames = downloadOrCopyWarsOnCleanStartUp(webappsDir, urlOfWarsToDeploy)

        def processStartString = "java -classpath ${classpath} ${jvmArgs()} ${EmbeddedTomcat.class.name} ${httpPort} " +
                "$tomcatbasedir ${warnames.join(",")}"

        if (ssl != null) {
            processStartString += " $ssl.port $ssl.cert"
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

    private static String[] downloadOrCopyWarsOnCleanStartUp(File webappsDirectory, WarPath[] warPaths) {
        for (WarPath path : warPaths) {
            if (new File("$webappsDirectory/$path.warname").exists()) {
                println " -- skipping downloading $path.warname as it already exists; to download the war again pass -DcleanET"
            } else {
                downloadWar(path, webappsDirectory)
            }
        }

        warPaths.collect { it.warname }
    }

    private static void downloadWar(WarPath path, File webappsDirectory) {
        if(!webappsDirectory.exists()) {
            webappsDirectory.mkdirs()
        }
        if (path.isUrl())
        {
            println("-- Downloading $path.value")
            new FileOutputStream("$webappsDirectory/$path.warname").write(path.value.openStream().bytes)
        } else {
            println "-- coping $path.value to $webappsDirectory"
            "cp $path.value $webappsDirectory".execute().waitFor()
        }
    }

    private static void logOutput(Process process, String tomcatbasedir) {
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
        if (ssl && ssl.truststore) {
            jvmArgs += " -Djavax.net.ssl.trustStore=$ssl.truststore.path -Djavax.net.ssl.trustStorePassword=$ssl.truststore.password"
        }
        jvmArgs
    }
}
