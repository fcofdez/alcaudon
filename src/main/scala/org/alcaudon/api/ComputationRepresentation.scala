package org.alcaudon.api

case class ComputationRepresentation(computationClassName: String,
                                     inputStreams: List[String],
                                     outputStreams: List[String])
