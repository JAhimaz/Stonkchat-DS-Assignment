package com.hep88
import com.hep88.protocol.JsonSerializable
import akka.actor.typed.ActorRef
import com.hep88.model.SubGroupActor

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
