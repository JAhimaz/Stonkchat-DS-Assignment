package com.hep88.view


import akka.actor.typed.ActorRef
import com.hep88.model.Account
import scalafx.event.ActionEvent
import scalafx.scene.control.{Alert, TextField, PasswordField}
import scalafx.stage.Stage
import scalafxml.core.macros.sfxml

import scala.util.{Failure, Success}
import com.hep88.Client
import com.hep88.ChatClient

@sfxml
class RegistryController(
                        private val usernameField : TextField,
                        private val passwordField: PasswordField,
                        private val confirmPasswordField: PasswordField
                        ) {

    var dialogStage: Stage = null
    var chatClientRef: Option[ActorRef[ChatClient.Command]] = None



    def handleSignUp(action:ActionEvent): Unit= {
        if (isInputValid) {
            chatClientRef map (_ ! ChatClient.RegisterAttempt(usernameField.text(),passwordField.text()))
        }
    }

    def successfulRegister():Unit={
        val alert = new Alert(Alert.AlertType.Information){
            initOwner(dialogStage)
            title = "Create Success"
            headerText= "Account saved and created"
            contentText=  "Account saved and created"
        }.showAndWait()
        Client.showLogIn()
    }

    def failedRegister():Unit={
        val alert = new Alert(Alert.AlertType.Warning) {
            initOwner(dialogStage)
            title = "Failed to Save"
            headerText = "Database Error"
            contentText = "Username has been taken!"
        }.showAndWait()
    }


    def handleCancel(action:ActionEvent)={
        Client.showLogIn()
    }

  def nullChecking(x: String) = x == null || x.length == 0

  def isInputValid(): Boolean = {
    var errorMessage = ""

    if (nullChecking(usernameField.text.value)) {
      errorMessage += "Username field is empty!!\n"
    }
 
    if (nullChecking(passwordField.text.value)) {
      errorMessage += "Password field is empty!\n"
    }
    if (nullChecking(confirmPasswordField.text.value)) {
      errorMessage += "Confirm Password field is empty!\n"
    }
    if(passwordField.text.value!= confirmPasswordField.text.value){
      errorMessage += "Passwords doesn't match!"
    }
    if (errorMessage.length() == 0) {
      return true
    }
    else {
      val alert = new Alert(Alert.AlertType.Error) {
        initOwner(dialogStage)
        title = "Invalid Fields!"
        headerText = "Please correct invalid fields"
        contentText = errorMessage
      }.showAndWait()
      return false
    }
  }







}