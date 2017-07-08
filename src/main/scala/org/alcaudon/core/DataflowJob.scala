package org.alcaudon.core

import java.net.URI

case class JarInfo(key: String, uri: URI)

case class DataflowJob(id: String, _requiredJars: List[URI]) {
  def requiredJars: List[JarInfo] = {
    _requiredJars.map(uri => JarInfo(s"$id-$uri", uri))
  }
}
