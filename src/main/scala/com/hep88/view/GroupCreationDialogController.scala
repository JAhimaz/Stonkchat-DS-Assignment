package com.hep88.view

import javafx.{scene => jfxs}
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import akka.actor.typed.ActorRef
import scalafx.Includes._
import scalafx.scene.control.{Label, TextField, Alert}
import scalafx.stage.Stage
import com.hep88.ChatClient
import com.hep88.Client


@sfxml
class GroupCreationDialogController(
    private val groupNameField:TextField
){

    private var _gname : String = ""
    var dialogStage : Stage = null
    var okClicked = false
    var chatClientRef: Option[ActorRef[ChatClient.Command]] = None

    def gname = _gname
    
    def handleCancel(action: ActionEvent){
        dialogStage.close()
    }

    def handleOk(action: ActionEvent){
        var errorMessage=""

        if(nullChecking(groupNameField.text.value)){
            errorMessage += "Group name cant be empty!"
        }

        if(errorMessage.length()==0){
            Client.mainController.get.gname = groupNameField.text.value
            okClicked = true
            dialogStage.close()
        }
        else{
            // Show the error message.
            val alert = new Alert(Alert.AlertType.Error){
            initOwner(dialogStage)
            title = "Invalid Fields"
            headerText = "Please correct invalid fields"
            contentText = errorMessage
            }.showAndWait()
        }
    }

    def nullChecking (x : String) = x == null || x.length == 0
}
