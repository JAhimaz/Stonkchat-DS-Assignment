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
import scala.util.{Failure, Success}


object ChatServer {
  //setupDB -> uncomment if server running. 
  //comment if client running
  //Database.setupDB()
  

  sealed trait Command extends JsonSerializable
  //protocol 
  case class JoinChat(name: String, from: ActorRef[ChatClient.Command]) extends Command
  case class Leave(name: String, from: ActorRef[ChatClient.Command]) extends Command
  //database protocol
  case class LogIn(username: String,password: String, from: ActorRef[ChatClient.Command]) extends Command
  case class RegisterUser(username: String,password: String,from: ActorRef[ChatClient.Command]) extends Command
  //state 
  val ServerKey: ServiceKey[ChatServer.Command] = ServiceKey("Server")
  val members = new ObservableHashSet[User]() //unique user
    //ns = observableHashset
    members.onChange{(ns, _) =>
    for(member <- ns){
      member.ref ! ChatClient.MemberList(members.toList)
    }
  }


  def apply(): Behavior[ChatServer.Command] = Behaviors.setup { context =>

    context.system.receptionist ! Receptionist.Register(ServerKey, context.self)
    
    Upnp.bindPort(context)

    Behaviors.receiveMessage { message =>
      message match {
        case LogIn(username,password,from)=>
            val account = new Account(username,password)
            from ! ChatClient.LogInResult(account.isVerify)
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
        case JoinChat(name, from) =>
            members += User(name, from)
            from ! ChatClient.Joined(members.toList)
            Behaviors.same
          case Leave(name, from) => 
            members -= User(name, from)
            Behaviors.same
        }
      }
  }
}

object Server extends App {
  
  val config = ConfigFactory.load()
  val mainSystem = akka.actor.ActorSystem("HelloSystem", MyConfiguration.askForConfig().withFallback(config))
  val typedSystem: ActorSystem[Nothing] = mainSystem.toTyped
  val cluster = Cluster(typedSystem)
  cluster.manager ! Join(cluster.selfMember.address)
  AkkaManagement(mainSystem).start()
  //val serviceDiscovery = Discovery(mainSystem).discovery
  ClusterBootstrap(mainSystem).start() 
  //val greeterMain: ActorSystem[ChatServer.Command] = ActorSystem(ChatServer(), "HelloSystem")
  mainSystem.spawn(ChatServer(), "ChatServer")  
}
