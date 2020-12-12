package com.stonk
import com.stonk.protocol.JsonSerializable
import akka.actor.typed.ActorRef
import com.stonk.model.SubGroupActor

case class User(name: String, ref: ActorRef[ChatClient.Command])extends JsonSerializable {
  override def toString: String = {
    name
  }
}

case class Group(name: String, ref: ActorRef[SubGroupActor.Command]) extends JsonSerializable{
    override def toString: String = {
    name
  }
}
