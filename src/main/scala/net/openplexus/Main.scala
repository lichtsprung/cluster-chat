package net.openplexus

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{Member, MemberStatus, Cluster}
import akka.cluster.ClusterEvent.MemberJoined
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.MemberUnreachable

object ClusterSeedMain {

  def main(args: Array[String]): Unit = {

    // Override the configuration of the port
    // when specified as program argument
    if (args.nonEmpty) System.setProperty("akka.remote.netty.port", args(0))

    // Create an Akka system
    val system = ActorSystem("ClusterSystem")
    val clusterListener = system.actorOf(Props(new Actor with ActorLogging {
      def receive = {
        case state: CurrentClusterState ⇒
          log.info("Current members: {}", state.members)
        case MemberJoined(member) ⇒
          log.info("Member joined: {}", member)
        case MemberUp(member) ⇒
          log.info("Member is Up: {}", member)
        case MemberUnreachable(member) ⇒
          log.info("Member detected as unreachable: {}", member)
        case _: ClusterDomainEvent ⇒ // ignore

      }
    }), name = "clusterListener")

    Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])
  }

}

object ChatMain {
  def main(args: Array[String]) {
    val system = ActorSystem("Chat Client")
    val client = system.actorOf(Props[ChatClient], "chatclient")
    client ! PushMessage("Ping")
  }
}


case object RegisterClient

case class PrintMessage(msg: String)

case class PushMessage(Msg: String)

class ChatClient extends Actor {
  var clients = IndexedSeq.empty[ActorRef]

  override def preStart() {
    Cluster(context.system).subscribe(self, classOf[MemberEvent])
  }

  override def postStop() {
    Cluster(context.system).unsubscribe(self)
  }


  def receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case RegisterClient if !clients.contains(sender) =>
      println("Added client: " + sender)
      context watch sender
      clients = clients :+ sender
    case Terminated(actor) =>
      clients = clients.filterNot(_ == actor)
    case PrintMessage(msg) =>
      println(msg)
    case PushMessage(msg) =>
      clients.foreach(a => {
        println("Pushing message to " + a)
        a ! PrintMessage(msg)
      })
      self ! PrintMessage(msg)
  }

  def register(member: Member) {
    println("Registering: " + RootActorPath(member.address) / "user" / "chatclient")

    context.actorFor(RootActorPath(member.address) / "user" / "chatclient") ! RegisterClient
  }
}