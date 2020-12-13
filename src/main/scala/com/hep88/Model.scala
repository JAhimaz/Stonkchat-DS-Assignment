package com.hep88
import com.hep88.protocol.JsonSerializable
import akka.actor.typed.ActorRef

case class User(name: String, ref: ActorRef[ChatClient.Command])extends JsonSerializable {
  override def toString: String = {
    name
  }
}
