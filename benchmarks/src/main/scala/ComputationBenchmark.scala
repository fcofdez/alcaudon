package org.alcaudon

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(1)
@Threads(1)
@Warmup(iterations = 10, time = 15, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(iterations = 10, time = 20, timeUnit = TimeUnit.SECONDS, batchSize = 1)
class ComputationBenchmark {

  @Param(Array("1", "5", "50"))
  var throughPut = 0

  @Param(Array("affinity-dispatcher", "default-fj-dispatcher", "fixed-size-dispatcher"))
  var dispatcher = ""

  @Param(Array("SingleConsumerOnlyUnboundedMailbox")) //"default"
  var mailbox = ""

  final val numThreads, numActors = 8
  final val numQueriesPerActor = 400000
  final val totalNumberOfMessages = numQueriesPerActor * numActors
  final val numUsersInDB = 300000

  @Setup(Level.Trial)
  def setup(): Unit = {
  }

  @TearDown(Level.Trial)
  def shutdown(): Unit = {}

  @Setup(Level.Invocation)
  def setupActors(): Unit = {
  }

  @Benchmark
  @OperationsPerInvocation(totalNumberOfMessages)
  def queryUserServiceActor(): Unit = {
    val startNanoTime = System.nanoTime()
    1 + 1
  }
}
