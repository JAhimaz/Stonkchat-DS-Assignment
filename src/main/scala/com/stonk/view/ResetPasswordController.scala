package com.stonk.view

import javafx.{scene => jfxs}
import com.stonk.Client
import com.stonk.ChatClient

import akka.actor.typed.ActorRef
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.stage.Stage
import scalafxml.core.macros.sfxml
import scalafxml.core.{FXMLLoader, NoDependencyResolver}

import scalafx.scene.control.{Alert, TextField, Label}
import scalafx.stage.Stage


@sfxml
class ResetPasswordController(
    private val username: TextField,
    private val questionField: TextField,
    private val answerField: TextField,
    private val passwordField: TextField,
    private val confirmPasswordField: TextField,
    private val errorText:Label

){
    var dialogStage: Stage = null


    var chatClientRef : Option[ActorRef[ChatClient.Command]] = None

    def handleSubmit(action: ActionEvent):Unit={
        if(isInputValid){
            chatClientRef.get ! ChatClient.RequestChangePassword(answerField.text.value,passwordField.text.value,username.text.value)
        }
    }

    def resetInitializer(user:String,question:String):Unit={
        questionField.text = question

        username.text() =user
    }

    def handleCancel(action:ActionEvent)={
        Client.showLogIn()
    }

    def nullChecking(x: String) = x == null || x.length == 0

    def isInputValid():Boolean={
        var errorMessage=""

        if(nullChecking(answerField.text.value)){
            errorMessage += "Answer cant be empty!\n"
        }
        if(nullChecking(passwordField.text.value)){
            errorMessage += "New password cant be empty!\n"
        }
        if(nullChecking(confirmPasswordField.text.value)){
            errorMessage += "Please double confirm your new password!\n"
        }
        if(passwordField.text.value!= confirmPasswordField.text.value){
        errorMessage += "Passwords doesn't match!"
        }

        if(errorMessage.length()==0){
            return true
        }
        else{
            errorText.text=errorMessage
            return false
        }
    }

    def resetFailed():Unit={
        val alert = new Alert(Alert.AlertType.Error){
          initOwner(dialogStage)
          title = "Failed to change password"
          headerText = "Password is not changed"
          contentText = "A field you have entered may be incorrect"
        }.showAndWait()
    }

    def resetSuccess():Unit={
        val alert = new Alert(Alert.AlertType.Information){
          initOwner(dialogStage)
          title = "Successfully changed password"
          headerText = "Password has been changed"
          contentText = "You may now login with your new password"
        }.showAndWait()
        Client.showLogIn()
    }

}