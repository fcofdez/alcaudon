enablePlugins(DockerPlugin)

mainClass in assembly := Some("org.alcaudon.runtime.Main")

assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
  case "META-INF/aop.xml" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"
  val weaverVersion = "1.8.10"
  val weaverPath = s"/app/aspectjweaver-$weaverVersion.jar"
  val weaverURL = url(
    s"http://central.maven.org/maven2/org/aspectj/aspectjweaver/$weaverVersion/aspectjweaver-$weaverVersion.jar")

  new Dockerfile {
    from("java:8")
    addRaw(weaverURL, weaverPath)
    add(artifact, artifactTargetPath)
    entryPoint("java", s"-javaagent:$weaverPath", "-jar", artifactTargetPath)
  }
}

imageNames in docker := {
  val imageName = ImageName(namespace = Some("fcofdezc"),
                            repository = name.value,
                            tag = Some("v" + version.value))
  Seq(imageName, imageName.copy(tag = Some("latest")))
}
