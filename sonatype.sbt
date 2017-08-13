homepage := Some(url("https://github.com/fcofdez/alcaudon"))

scmInfo := Some(
  ScmInfo(url("https://github.com/fcofdez/alcaudon"),
          "git@github.com:fcofdez/alcaudon.git"))

developers += Developer("fcofdez",
                        "Francisco Fernandez Castaño",
                        "francisco.fernandez.castano@gmail.com",
                        url("https://github.com/fcofdez"))

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

publishMavenStyle := true

pomIncludeRepository := (_ => false)

pgpReadOnly := true

useGpg := true

pgpPublicRing := file("~/.gnupg/pubring.gpg")

pgpSecretRing := file("~/.gnupg/secring.gpg")

usePgpKeyHex("FDD1DC390A9EA6FD5D26B9FEB04976C54D31628E")

pgpPassphrase := Some(
  Option(System.getenv().get("PGP_PASSPHRASE")).getOrElse("").toCharArray)

sonatypeProfileName := "com.github.fcofdez"

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield
    Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      username,
      password
    )
).toSeq
