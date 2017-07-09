package org.alcaudon.api

import org.alcaudon.api.DataflowBuilder.AlcaudonInputStream

case class ComputationRepresentation(computationClassName: String,
                                     inputStreams: List[AlcaudonInputStream],
                                     outputStreams: List[String])
