package uk.co.o2.gradleplugin.multiwartomcat

class MultiwarTomcatStopper {

    public static void stop(def port) {
        "jps -ml".execute().text.split('\n').findAll {
            it.contains("${MultiwarTomcat.class.name}") && it.contains("$port")
        }.each {
            println "-- killing MultiwarTomcat on port $port"
            String pid = it.split(' ')[0]
            "kill -9 ${pid}".execute().waitFor()
        }
    }

}
