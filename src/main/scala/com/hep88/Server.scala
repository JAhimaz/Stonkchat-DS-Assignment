package com.hep88

import scalafx.collections.ObservableHashSet
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist,ServiceKey}
import akka.actor.typed.scaladsl.adapter._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.discovery.Discovery
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory
import com.hep88.protocol.JsonSerializable

import com.hep88.util.Database
import com.hep88.model.Account
import com.hep88.model.SubGroupActor
import scala.util.{Failure, Success}

object ChatServer {
 
  // ------------------------

  // Setting this to true will setup the Database
  // Set to false if client (Or when testing client)

  val isServerhost = false


  // ------------------------


  if(isServerhost){
    Database.setupDB()
  }
  
  sealed trait Command extends JsonSerializable
  //protocol 
  case class JoinChat(name: String, from: ActorRef[ChatClient.Command]) extends Command
  case class Leave(name: String, from: ActorRef[ChatClient.Command]) extends Command
  //database protocol
  case class LogIn(username: String,password: String, from: ActorRef[ChatClient.Command]) extends Command
  case class LogOut(username: String, from: ActorRef[ChatClient.Command]) extends Command
  case class RegisterUser(username: String,password: String,from: ActorRef[ChatClient.Command]) extends Command
  
  //group creation protocol
  case class GroupCreation(gname : String, name: String, gref: ActorRef[SubGroupActor.Command], from: ActorRef[ChatClient.Command] ) extends Command
  case class GroupLeave(gname: String, from : ActorRef[SubGroupActor.Command]) extends Command

  //state 
  val ServerKey: ServiceKey[ChatServer.Command] = ServiceKey("Server")
  val members = new ObservableHashSet[User]() //unique user


  val groups = new ObservableHashSet[Group]()


    //ns = observableHashset
    // maybe dont need this
    members.onChange{(ns, _) =>
    for(member <- ns){
      member.ref ! ChatClient.GroupList(groups.toList)
    }
  }

    
    groups.onChange{(ns, _) =>
    for(member <- members){
      member.ref ! ChatClient.GroupList(groups.toList)
    }

  }

  def apply(): Behavior[ChatServer.Command] = Behaviors.setup { context =>

    context.system.receptionist ! Receptionist.Register(ServerKey, context.self)
    
    Upnp.bindPort(context)

    Behaviors.receiveMessage { message =>
      message match {
        case LogIn(username,password,from)=>
            val account = new Account(username,password)
            if(account.isVerify){
              members+= User(username,from)
            }
            
            from ! ChatClient.LogInResult(account.isVerify,groups.toList)
            Behaviors.same

        case LogOut(username,from)=>
            members -= User(username,from)
            from ! ChatClient.LogOutResult
            Behaviors.same

        case RegisterUser(username,password,from)=>
            val account = new Account(username,password)
            if(account.isExist){
              from ! ChatClient.RegisterResult(false)
            }
            else{
              account.createAccount()
              from ! ChatClient.RegisterResult(true)
            }
            Behaviors.same
          case Leave(name, from) => 
            members -= User(name, from)
            Behaviors.same
          case GroupCreation(gname,name,gref,from)=>
            groups += Group(gname,gref)
            from ! ChatClient.JoinGroup(name,gref)
            Behaviors.same
          case GroupLeave(gname,from)=>
            groups -= Group(gname,from)
            Behaviors.same
        }
      }
  }
}

object Server extends App {
  
  val config = ConfigFactory.load()
  val mainSystem = akka.actor.ActorSystem("StonkChatSys", MyConfiguration.askForConfig().withFallback(config))
  val typedSystem: ActorSystem[Nothing] = mainSystem.toTyped
  val cluster = Cluster(typedSystem)
  cluster.manager ! Join(cluster.selfMember.address)
  AkkaManagement(mainSystem).start()
  
  ClusterBootstrap(mainSystem).start() 
  
  mainSystem.spawn(ChatServer(), "ChatServer")
}
