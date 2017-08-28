enablePlugins(DockerPlugin)

enablePlugins(JavaAppPackaging)

mainClass in Compile := Some("org.alcaudon.runtime.Main")

dockerBaseImage := "java:8"

dockerUpdateLatest := true

dockerUsername := Some("fcofdezc")
