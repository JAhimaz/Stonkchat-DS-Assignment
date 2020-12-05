package com.hep88.view
import akka.actor.typed.ActorRef
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.control.{Label, ListView, TextField,Alert}
import scalafx.stage.Stage



import com.hep88.ChatClient
import com.hep88.Group
import com.hep88.User
import com.hep88.Client
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
@sfxml
class MainWindowController(
  private val txtName: TextField,
  private val lblStatus: Label,
  private val listGroup: ListView[Group]
  ) {


  var chatClientRef: Option[ActorRef[ChatClient.Command]] = None

  val receivedText: ObservableBuffer[String] =  new ObservableBuffer[String]()

  var dialogStage : Stage  = null

  var gname:String=""


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
          Client.userRef ! ChatClient.CreateGroup(gname,Client.loggedInUser)
        }
    }

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
        headerText = "YOU DONT HAVE NOT CREATED A GROUP YET"
        contentText = "YOU CAN DISBAND YOUR GROUP WHEN YOU HAVE CREATED ONE"
        }.showAndWait()
    }

  }



}