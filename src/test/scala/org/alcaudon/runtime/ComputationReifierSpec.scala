package org.alcaudon.runtime

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.TestActorRef
import akka.util.Timeout
import org.scalatest.FunSuite
import org.scalacheck.Gen
import org.scalacheck.Gen.{const, frequency, identifier, nonEmptyListOf, oneOf, someOf}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.commands.Commands

import scala.util.{Success, Try}
import scala.collection.immutable.Map
import scala.concurrent.Await


object CommandsComputation extends org.scalacheck.Properties("CommandsComputation") {

  property("computationspec") = ComputationSpec.property()

}

object ComputationSpec extends Commands {

  implicit val as = ActorSystem()

  type Sut = TestActorRef[Echo]

  case class State (
                     contents: collection.immutable.Map[String,String],
                     deleted: collection.immutable.Set[String],
                     connected: Boolean
                   )

  def canCreateNewSut(newState: State, initSuts: Traversable[State],
                      runningSuts: Traversable[Sut]
                     ): Boolean = {
    initSuts.isEmpty && runningSuts.isEmpty
  }

  def destroySut(sut: Sut): Unit = {
    as.stop(sut)
//    sut.reconnect
//    sut.flushdb
//    sut.quit
  }

  def genInitialState: Gen[State] = State(
    collection.immutable.Map.empty,
    collection.immutable.Set.empty,
    true
  )

  class Echo extends Actor {
    var state = List[String]()
    def receive = {
      case 1 => sender() ! 2
      case key: String =>
        state = key :: state
        if (key.contains("t"))
          sender () ! 1
        else sender () ! 2
    }
  }

  def initialPreCondition(state: State): Boolean = state.connected

  def newSut(state: State): Sut = {
    TestActorRef[Echo]
  }

  def genCommand(state: State): Gen[Command] = {
    frequency(
      (50, genSet),
      (10, genSetExisting(state))
    )
  }

  //val genKey = arbitrary[String]
  //val genVal = arbitrary[String]
  val genKey = identifier
  val genVal = identifier

  val genSet: Gen[Set] = for {
    key <- genKey
    value <- genVal
  } yield Set(key, value)

//  def genDelExisting(state: State): Gen[Del] =
//    if(state.contents.isEmpty) genDel
//    else someOf(state.contents.keys.toSeq).map(Del)

  def genSetExisting(state: State): Gen[Set] =
    if(state.contents.isEmpty) genSet else for {
      key <- oneOf(state.contents.keys.toSeq)
      value <- oneOf(genVal, const(state.contents(key)))
    } yield Set(key,value)

//  val genGet: Gen[Get] = genKey.map(Get)
//
//  val genDel: Gen[Del] = nonEmptyListOf(genKey).map(Del)
//
//  def genGetExisting(state: State): Gen[Get] =
//    if(state.contents.isEmpty) genGet else for {
//      key <- oneOf(state.contents.keys.toSeq)
//    } yield Get(key)
//
//  def genGetDeleted(state: State): Gen[Get] =
//    if(state.deleted.isEmpty) genGet else for {
//      key <- oneOf(state.deleted.toSeq)
//    } yield Get(key)


//  case object DBSize extends Command {
//    type Result = Option[Long]
//    def run(sut: Sut) = sut.dbsize
//    def preCondition(state: State) = state.connected
//    def nextState(state: State) = state
//    def postCondition(state: State, result: Try[Option[Long]]) =
//      result == Success(Some(state.contents.keys.size))
//  }

  case class Set(key: String, value: String) extends Command {
    type Result = Int
    import akka.pattern.ask
    import scala.concurrent.duration._
    def run(sut: Sut) = {
      implicit val timeout = Timeout(2.seconds)
      val x = (sut ? key).mapTo[Int]
      Await.result(x, 2.seconds)
    }

    def preCondition(state: State) = state.connected
    def nextState(state: State) = state.copy(
      contents = state.contents + (key -> value),
      deleted = state.deleted.filter(_ != key)
    )

    def postCondition(state: State, result: Try[Int]) = {
        result == Success(2)
      }
  }

//  case class Del(keys: Seq[String]) extends Command {
//    type Result = Option[Long]
//    def run(sut: Sut) =
//      if(keys.isEmpty) Some(0)
//      else sut.del(keys.head, keys.tail: _*)
//    def preCondition(state: State) = state.connected
//    def nextState(state: State) = state.copy(
//      contents = state.contents -- keys,
//      deleted = state.deleted ++ keys
//    )
//    def postCondition(state: State, result: Try[Option[Long]]) =
//      result == Success(Some(state.contents.filterKeys(keys.contains).size))
//  }
//
//  case object FlushDB extends Command {
//    type Result = Boolean
//    def run(sut: Sut) = sut.flushdb
//    def preCondition(state: State) = state.connected
//    def nextState(state: State) = state.copy(
//      contents = Map.empty
//    )
//    def postCondition(state: State, result: Try[Boolean]) =
//      result == Success(true)
//  }
//
//  case object ToggleConnected extends Command {
//    type Result = Boolean
//    def run(sut: Sut) = {
//      if(sut.connected) sut.quit
//      else sut.connect
//    }
//    def preCondition(state: State) = true
//    def nextState(state: State) = state.copy(
//      connected = !state.connected
//    )
//    def postCondition(state: State, result: Try[Boolean]) =
//      result == Success(true)
//  }
//
//  case class Get(key: String) extends Command {
//    type Result = Option[String]
//    def run(sut: Sut) = sut.get(key)
//    def preCondition(state: State) = state.connected
//    def nextState(state: State) = state
//    def postCondition(state: State, result: Try[Option[String]]) =
//      result == Success(state.contents.get(key))
//  }

  //  case class BitCount(key: String) extends Command {
  //    type Result = Option[Int]
  //    def run(sut: Sut) = sut.bitcount(key, None)
  //    def preCondition(state: State) = state.connected
  //    def nextState(state: State) = state
  //    def postCondition(state: State, result: Try[Option[Int]]) = {
  //      val expected = state.contents.get(key) match {
  //        case None => 0
  //        case Some(str) bitcount(str)
  //      result == Success(state.contents.get(key))
  //  }

}
