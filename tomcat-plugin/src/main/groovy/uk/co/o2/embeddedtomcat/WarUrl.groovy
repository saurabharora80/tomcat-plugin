package uk.co.o2.embeddedtomcat

/**
 * Created by ee on 17/07/14.
 */
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
