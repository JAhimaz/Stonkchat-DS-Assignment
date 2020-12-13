package com.stonk.view

import javafx.{scene => jfxs}
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import akka.actor.typed.ActorRef
import scalafx.Includes._
import scalafx.scene.control.{Label, TextField, Alert}
import scalafx.stage.Stage
import com.stonk.ChatClient
import com.stonk.Client

@sfxml
class UserNameRequestController(
    private val userNameField: TextField,
    private val errorText: Label

){
    var dialogStage: Stage = null
    var okClicked = false
    var chatClientRef: Option[ActorRef[ChatClient.Command]] = None

    def handleCancel(action: ActionEvent){
        dialogStage.close()
    }

    def handleSubmit(action: ActionEvent){
        var errorMessage=""

        if(nullChecking(userNameField.text.value)){
            errorMessage += "Username cant be empty!"
        }

        if(errorMessage.length()==0){
            chatClientRef.get ! ChatClient.CheckUserExist(userNameField.text.value)
            dialogStage.close()
        }
        else{
            errorText.text=errorMessage
        }



    }


    def nullChecking (x : String) = x == null || x.length == 0

}