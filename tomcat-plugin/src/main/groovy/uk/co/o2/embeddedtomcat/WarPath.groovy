package uk.co.o2.embeddedtomcat

/**
 * Created by ee on 17/07/14.
 */
class WarPath {
    private String path

    WarPath(String url) {
        this.path = url
    }

    def getValue() {
        try {
            return path.toURL()
        } catch (MalformedURLException expection) {
            return path
        }
    }

    String getWarname() {
        path.split("/").last()
    }

    boolean isUrl() {
        getValue().class.isAssignableFrom(URL)
    }
}
