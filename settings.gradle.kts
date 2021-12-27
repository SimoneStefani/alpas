include("framework")
include("pulsar")

rootProject.name = "alpas"
project(":framework").name = "alpas"

rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
}
