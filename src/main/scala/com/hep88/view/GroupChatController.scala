package com.hep88.view

import javafx.{scene => jfxs}
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.stage.Stage
import akka.actor.typed.ActorRef
import scalafx.Includes._
import scalafx.collections.ObservableBuffer

import scalafx.scene.control.{Label, TextField,ListView,Alert}

import com.hep88.Client
import com.hep88.ChatClient
import com.hep88.model.SubGroupActor

@sfxml
class GroupChatController(
    private val groupName : Label,
    private val listMsg : ListView[String],
    private val textMsg : TextField,
){

    //var groupChatOwner : Option[Address] = None
    var chatClientRef : Option[ActorRef[ChatClient.Command]] = None
    var groupRef : Option[ActorRef[SubGroupActor.Command]] = None
    var dialogStage : Stage  = null
    var userMsg : String = ""

    val receivedText: ObservableBuffer[String] =  new ObservableBuffer[String]()

    listMsg.items = receivedText

    def receiveGroupName(gname: String) : Unit = {
        groupName.text() = gname
    }

    def setGroupRef(gref : Option[ActorRef[SubGroupActor.Command]]) : Unit = {
        groupRef = gref
    }

    def showMemberList(action : ActionEvent) : Unit = {
        groupRef.get ! SubGroupActor.DisplayMembers(chatClientRef.get)
    }

    def backBut(action : ActionEvent) : Unit = {
        Client.showMainChat()
        Client.userRef ! ChatClient.LeaveGroup(groupRef.get)
    }

    def handleSend(actionEvent : ActionEvent) : Unit = {
        userMsg = Client.loggedInUser + ": " +  textMsg.text()
        Client.userRef ! ChatClient.SendMessageL(ChatClient.groupRefOpt.get,userMsg)
    }

    def addText(text : String) : Unit = {
        receivedText += text
    }


    def ownerLeft() : Unit = {
        val alert = new Alert(Alert.AlertType.Warning) {
            initOwner(dialogStage)
            title = "SOMETHING WENT WRONG"
            headerText = "GROUP OWNER OR SERVER IS DOWN"
            contentText = "Please choose or create another group"
        }.showAndWait()
        ChatClient.groupRefOpt = None
        ChatClient.groupOwnerAddress = None
        Client.showMainChat()
    }
}
