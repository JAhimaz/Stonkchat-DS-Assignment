package com.stonk.model


import scalafx.collections.ObservableHashSet
import akka.actor.typed.{PostStop,ActorRef}
import com.stonk.protocol.JsonSerializable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.stonk.ChatClient
import com.stonk.User
import akka.actor.Address
import com.stonk.ChatServer


object SubGroupActor {
  
  sealed trait Command extends JsonSerializable
  //protocol 
  case class JoinChat(name: String, from: ActorRef[ChatClient.Command]) extends Command
  case class Leave(name: String, from: ActorRef[ChatClient.Command]) extends Command
  case class Initializer(owner: Option[Address], gname : String, sRef : ActorRef[ChatServer.Command]) extends Command
  case class GroupMessage(msg: String) extends Command
  case class CloseGroup(from: ActorRef[ChatClient.Command]) extends Command
  case class DisplayMembers(from: ActorRef[ChatClient.Command] ) extends Command


  //state 
  val members = new ObservableHashSet[User]() //unique user

  var groupOwner: Option[Address] = None

  var groupName: String=""

  var serverRef : Option[ActorRef[ChatServer.Command]] = None

  def apply(): Behavior[this.Command] = Behaviors.setup { context =>

    Behaviors.receiveMessage[SubGroupActor.Command] { message =>
      message match {
        case Initializer(address,gname,remoteOpt)=>
            groupOwner= address
            groupName= gname
            serverRef= Option(remoteOpt)
            Behaviors.same
        case JoinChat(name, from) =>
            members += User(name, from)
            from ! ChatClient.Joined(groupOwner,groupName)
            Behaviors.same
        case Leave(name, from) => 
            members -= User(name, from)
            Behaviors.same

        case SubGroupActor.GroupMessage(content) =>
            for(member <- members){
              member.ref ! ChatClient.Message(content)
            }
            Behaviors.same


        case DisplayMembers(from)=>
          from ! ChatClient.DisplayMemberList(members.toList)
          Behaviors.same


        case CloseGroup(from) =>
          for(member <- members){
            member.ref ! ChatClient.GroupOwnerLeft
          }
          members.clear()
          serverRef.get ! ChatServer.GroupLeave(groupName,context.self)
          Behaviors.same

        }
      }.receiveSignal{
        case (context, PostStop) =>
          for(member <- members){
            member.ref ! ChatClient.GroupOwnerLeft
          }
          serverRef.get ! ChatServer.GroupLeave(groupName,context.self)
          Behaviors.same
      }
  }

}



