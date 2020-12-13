package com.stonk.view
import akka.actor.typed.ActorRef
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.stage.Stage

import scalafx.scene.control.{Label, ListView, TextField,Alert}
import scalafx.scene.layout.{Pane}

import scalafx.scene.input.MouseEvent

import com.stonk.ChatClient
import com.stonk.Group
import com.stonk.User
import com.stonk.Client
import scalafx.collections.ObservableBuffer
import scalafx.Includes._

@sfxml
class MainWindowController(
    private val txtName: Label,
    private val listGroup: ListView[Group],
    private val guidePane : Pane
  ) {


  var chatClientRef: Option[ActorRef[ChatClient.Command]] = None
  val receivedText: ObservableBuffer[String] =  new ObservableBuffer[String]()
  var dialogStage : Stage  = null
  var gname : String = ""
  var gOwner : String = ""

  def openInfo(action : MouseEvent) : Unit = {
    guidePane.visible = true
  }

  def closeInfo(action : MouseEvent) : Unit = {
    guidePane.visible = false
  }


  txtName.text() = Client.loggedInUser

  
  def updateList(x: Iterable[Group]): Unit ={
    listGroup.items = new ObservableBuffer[Group]() ++= x
  }

  def handleJoinGroup(action : ActionEvent): Unit={
    if (listGroup.selectionModel().selectedIndex.value >= 0){
          Client.userRef ! ChatClient.JoinGroup(Client.loggedInUser,listGroup.selectionModel().selectedItem.value.ref)
        }
    else{
        val alert = new Alert(Alert.AlertType.Warning) {
        initOwner(dialogStage)
        title = "NO GROUPS SELEECTED"
        headerText = "YOU HAVE NOT SELECTED A GROUP TO JOIN"
        contentText = "PLEASE SELECT A GROUP TO JOIN IN THE LIST OF GROUPS"
        }.showAndWait()
    }
  }

  


  def createGroup(actionEvent: ActionEvent):Unit={

    if(ChatClient.groupCreated==true){
        val alert = new Alert(Alert.AlertType.Warning) {
        initOwner(dialogStage)
        title = "ALREADY HAVE GROUP"
        headerText = "ONLY 1 GROUP ALLOWED"
        contentText = "PLEASE DISBAND CURRENT GROUP BEFORE CREATING NEW ONE"
        }.showAndWait()
    }
    else{
        val okClicked = Client.showGroupCreationDialog()
        if(okClicked){
          Client.userRef ! ChatClient.CreateGroup(gname , Client.loggedInUser)
        }
    }

  }

  def logOut(action: ActionEvent):Unit={
    Client.userRef ! ChatClient.LogOutAttempt(Client.loggedInUser)
  }



  def disbandGroup(action: ActionEvent): Unit={
    if(ChatClient.groupCreated==true){
      Client.userRef ! ChatClient.DisbandGroup
      ChatClient.groupCreated=false
    }
    else{
        val alert = new Alert(Alert.AlertType.Warning) {
        initOwner(dialogStage)
        title = "NO GROUPS CREATED"
        headerText = "YOU HAVE NOT CREATED A GROUP YET"
        contentText = "YOU CAN DISBAND YOUR GROUP WHEN YOU HAVE CREATED ONE"
        }.showAndWait()
    }

  }



}