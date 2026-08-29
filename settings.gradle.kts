pluginManagement {
    includeBuild("../plugin-multiplexer/network")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("masonry-api")
include("masonry-common")
include("masonry-paper")
include("masonry-test")
