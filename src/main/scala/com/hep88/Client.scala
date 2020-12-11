package com.hep88
import akka.actor.typed.{ActorRef, PostStop, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist,ServiceKey}
import akka.cluster.typed._
import akka.{ actor => classic }
import akka.discovery.{Discovery,Lookup, ServiceDiscovery}
import akka.discovery.ServiceDiscovery.Resolved
import akka.actor.typed.scaladsl.adapter._
import com.hep88.protocol.JsonSerializable
import scalafx.collections.ObservableHashSet
import scalafx.application.Platform
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.Address
import akka.actor.typed.ActorSystem
import akka.actor.PoisonPill

import com.hep88.model.SubGroupActor


object ChatClient {
    sealed trait Command extends JsonSerializable
    //main protocol
    case object start extends Command

    //GUI protocol
    case class StartJoin(name: String) extends Command
    final case class SendMessageL(target: ActorRef[SubGroupActor.Command], content: String) extends Command
    case class LogInAttempt(username : String,password : String) extends Command
    case class RegisterAttempt(username: String,password : String) extends Command
    case class LogOutAttempt(username: String) extends Command

   
    //chat protocol 
    final case class MemberList(list: Iterable[User]) extends Command
    final case class GroupList(list: Iterable[Group]) extends Command
    final case class Joined(groupOwner: Option[Address], gname : String ) extends Command
    final case class Message(msg: String) extends Command

    //chat protocol login
    final case class LogInResult(error: Int, validity : Boolean, list: Iterable[Group] ) extends Command

    //chat protocol register
    final case class RegisterResult(validity: Boolean) extends Command
    final case object LogOutResult extends Command

    //group chat protocol
    final case class CreateGroup(gname : String,name : String) extends Command
    final case class JoinGroup(name: String, gref: ActorRef[SubGroupActor.Command]) extends Command
    final case object GroupOwnerLeft extends Command
    final case object DisbandGroup extends Command
    final case class LeaveGroup(gref: ActorRef[SubGroupActor.Command] ) extends Command
    final case class DisplayMemberList(list: Iterable[User]) extends Command
  

    //cluster receptionist
    final case object FindTheServer extends Command
    private case class ListingResponse(listing: Receptionist.Listing) extends Command

    //cluster reachability
    private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Command


    //state
    var defaultBehavior: Option[Behavior[ChatClient.Command]] = None
    var remoteOpt: Option[ActorRef[ChatServer.Command]] = None
    var nameOpt: Option[String] = None
    val members = new ObservableHashSet[User]()
    val unreachables = new ObservableHashSet[Address]()
    val groups = new ObservableHashSet[Group]()
    var groupRefOpt: Option[ActorRef[SubGroupActor.Command]] = None
    var createdGroupRefOpt : Option[ActorRef[SubGroupActor.Command]] = None
    var groupOwnerAddress: Option[Address] = None
    var groupName:String=""
    var groupCreated = false

    
    unreachables.onChange{(ns, _) =>
        Platform.runLater {
            Client.mainController map (_.updateList(groups.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address))))
            //Client.mainController.get.updateList(members.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address)))
        }
    }

    groups.onChange{(ns, _) =>
        Platform.runLater {
            Client.mainController map (_.updateList(groups.toList.filter(y => ! unreachables.exists (x => x == y.ref.path.address))))
        }
    }

    def apply(): Behavior[ChatClient.Command] =
        Behaviors.setup { context =>

        Upnp.bindPort(context)
        
       // (2) create an ActorRef that can be thought of as a Receptionist
        // Listing “adapter.” this will be used in the next line of code.
        // the ChatClient.ListingResponse(listing) part of the code tells the
        // Receptionist how to get back in touch with us after we contact
        // it in Step 4 below.
        // also, this line of code is long, so i wrapped it onto two lines
        val listingAdapter: ActorRef[Receptionist.Listing] =
            context.messageAdapter ( ChatClient.ListingResponse )
            
        //(3) send a message to the Receptionist saying that we want
        // to subscribe to events related to ChatServer.ServerKey, which
        // represents the ChatClient actor.
        context.system.receptionist ! Receptionist.Subscribe(ChatServer.ServerKey, listingAdapter)

        val reachabilityAdapter = context.messageAdapter(ReachabilityChange)
        Cluster(context.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

        //context.actorOf(RemoteRouterConfig(RoundRobinPool(5), addresses).props(Props[ChatClient.TestActorClassic]()), "testA")
        defaultBehavior = Some(Behaviors.receiveMessage[ChatClient.Command] { message =>
            message match {
                
                case ChatClient.start =>
                    context.self ! FindTheServer 
                    Behaviors.same
                // (4) send a Find message to the Receptionist, saying
                    // that we want to find any/all listings related to
                    // Mouth.MouthKey, i.e., the Mouth actor.
                case FindTheServer =>
                    println(s"Clinet Hello: got a FindTheServer message")
                    context.system.receptionist !
                        Receptionist.Find(ChatServer.ServerKey, listingAdapter)

                    Behaviors.same
                    // (5) after Step 4, the Receptionist sends us this
                    // ListingResponse message. the `listings` variable is
                    // a Set of ActorRef of type ChatServer.Command, which
                    // you can interpret as “a set of ChatServer ActorRefs.” for
                    // this example i know that there will be at most one
                    // ChatServer actor, but in other cases there may be more
                    // than one actor in this set.
                case ListingResponse(ChatServer.ServerKey.Listing(listings)) =>
                    val xs: Set[ActorRef[ChatServer.Command]] = listings
                    for (x <- xs) {
                        remoteOpt = Some(x)
                    }
                    Behaviors.same
                
                case RegisterAttempt(username,password)=>
                    remoteOpt.map (_ ! ChatServer.RegisterUser(username,password,context.self))
                    Behaviors.same

                case ChatClient.RegisterResult(result)=>
                    if(result==true){
                        Platform.runLater{
                            Client.registerController.get.successfulRegister()
                        }
                    }
                    else{
                        Platform.runLater{
                            Client.registerController.get.failedRegister()
                        }
                    }
                    Behaviors.same
  

                case LogInAttempt(username,password)=>
                    remoteOpt.map (_ ! ChatServer.LogIn(username,password,context.self))
                    Behaviors.same
                    

                case LogOutAttempt(username)=>
                    if(ChatClient.groupCreated==true){
                        createdGroupRefOpt.get ! SubGroupActor.CloseGroup(context.self)
                        ChatClient.groupCreated=false
                    }
                    remoteOpt.map (_ ! ChatServer.LogOut(username,context.self))
                    Behaviors.same


                case LogOutResult =>
                    Platform.runLater{
                        Client.showLogIn
                    }
                    Behaviors.same

                case ChatClient.LogInResult(errorcode,result,x)=>
                    if(result==true){
                        Platform.runLater{
                            Client.loginController.get.successfulLogin()
                        }
                        groups.clear()
                        groups ++= x

                    }
                    else{
                        Platform.runLater{
                            Client.loginController.get.failedLogin(errorcode)
                        }
                    }
                    Behaviors.same


                case CreateGroup(gname,name)=>
                    // help make sure doesnt repeat
                    val r = scala.util.Random
                    val groupRef = Client.mainSystem.spawn(SubGroupActor(),r.nextInt.toString)
                    groupRef ! SubGroupActor.Initializer(Option(context.system.address),gname,remoteOpt.get)
                    remoteOpt.map ( _ ! ChatServer.GroupCreation(gname,name, groupRef,context.self))
                    groupCreated=true
                    createdGroupRefOpt = Option(groupRef)
                    Behaviors.same

                case ChatClient.JoinGroup(name,groupRef)=>
                    nameOpt = Option(name)
                    groupRefOpt = Option(groupRef)
                    groupRef ! SubGroupActor.JoinChat(name,context.self)
                    Behaviors.same


                case DisbandGroup =>
                    createdGroupRefOpt.get ! SubGroupActor.CloseGroup(context.self)
                    Behaviors.same


                case DisplayMemberList(list: Iterable[User])=>
                    Platform.runLater{
                        Client.showMemberListDialog(list.toList)
                    }
                    Behaviors.same




                case LeaveGroup(gref) =>
                    gref ! SubGroupActor.Leave(nameOpt.get,context.self)
                    groupOwnerAddress= None
                    groupName=""
                    Behaviors.same

                case GroupList(list : Iterable[Group])=>
                    groups.clear()
                    groups ++= list
                    Behaviors.same
                    
                case ChatClient.Joined(groupOwner,gname) =>
                    groupOwnerAddress = groupOwner
                    groupName= gname
                    Platform.runLater {
                        Client.showGroupChat()
                        Client.groupChatController.get.setGroupRef(groupRefOpt)
                    }
                    Behaviors.same

                case SendMessageL(target, content) =>
                    target ! SubGroupActor.GroupMessage(content)
                    Behaviors.same
                case ChatClient.Message(msg) =>
                    Platform.runLater {
                        Client.groupChatController.get.addText(msg)

                    }
                    Behaviors.same

                case GroupOwnerLeft =>
                    Platform.runLater{
                        Client.groupChatController.get.ownerLeft()
                    }
                    Behaviors.same

                //process omission
                case ReachabilityChange(reachabilityEvent) =>
                reachabilityEvent match {
                    case UnreachableMember(member) =>
                        if(groupOwnerAddress.get == member.address){
                            Platform.runLater {
                                Client.groupChatController.get.ownerLeft()
                            }
                        }
                        unreachables += member.address
                        Behaviors.same
                    case ReachableMember(member) =>
                        unreachables -= member.address
                        Behaviors.same
                }                    
                case _=>
                    Behaviors.unhandled
            }
        }.receiveSignal{
            case (context, PostStop) =>
                for (name <- nameOpt;
                    remote <- remoteOpt){
                    remote ! ChatServer.Leave(name, context.self)
                    if(groupRefOpt != None){
                        groupRefOpt.get ! SubGroupActor.Leave(name,context.self)
                    }
                }

                
                Behaviors.same
        })
        defaultBehavior.get

    }
}
