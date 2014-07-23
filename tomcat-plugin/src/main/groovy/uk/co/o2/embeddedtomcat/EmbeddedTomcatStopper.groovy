package uk.co.o2.embeddedtomcat

class EmbeddedTomcatStopper {

    public static void stop(def port) {
        String embeddedTomcatProcess = "jps -ml".execute().text.split('\n').find {
            it.contains("${EmbeddedTomcat.class.name}") && it.contains("$port")
        }
        if (embeddedTomcatProcess) {
            println "-- killing embeddedTomcat"
            String pid = embeddedTomcatProcess.split(' ')[0]
            "kill -9 ${pid}".execute().waitFor()
        }
    }

}
