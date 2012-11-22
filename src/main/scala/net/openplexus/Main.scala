package net.openplexus

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{Member, MemberStatus, Cluster}

object ClusterSeedMain {

  def main(args: Array[String]) {

    if (args.nonEmpty) System.setProperty("akka.remote.netty.port", args(0))

    // Create an Akka system
    val system = ActorSystem("cluster")
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
    Cluster(system).join(Address("akka", "ClusterSystem", "fluxx", 2551))
    client ! PushMessage("Ping")
  }
}


case object RegisterClient
case object UnregisterClient

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
      state.members.filter(_.status == MemberStatus.Up) foreach(m => {
        println("Sending register message to " + m.address)
        register(m)
      })
    case state: MemberJoined =>
      println("Sending register message to " + state.member.address)
      register(state.member)
    case state: MemberLeft =>
      println("Sending unregister message to " + state.member.address)
      unregister(state.member)
    case RegisterClient if !clients.contains(sender) =>
      println("Added client: " + sender)
      context watch sender
      clients = clients :+ sender
    case UnregisterClient if clients.contains(sender) =>
      println("Removed client: " + sender)
      context unwatch sender
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

  def unregister(member: Member){
    println("Registering: " + RootActorPath(member.address) / "user" / "chatclient")
  }
}