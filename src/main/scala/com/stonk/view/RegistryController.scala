package com.stonk.view

import java.util.regex.Pattern;

import akka.actor.typed.ActorRef
import com.stonk.model.Account
import scalafx.event.ActionEvent
import scalafx.scene.control.{Alert, TextField, PasswordField}
import scalafx.scene.text.Text
import scalafx.stage.Stage
import scalafxml.core.macros.sfxml

import scala.util.{Failure, Success}
import com.stonk.Client
import com.stonk.ChatClient

@sfxml
class RegistryController(
      private val usernameField : TextField,
      private val passwordField: PasswordField,
      private val confirmPasswordField: PasswordField,
      private val securityQuestionField: TextField,
      private val securityAnswerField: TextField,
      private val errorText: Text
    ) {

    var dialogStage: Stage = null
    var chatClientRef: Option[ActorRef[ChatClient.Command]] = None

    errorText.text = ""

    def handleSignUp(action:ActionEvent): Unit= {
        if (isInputValid) {
            chatClientRef map (_ ! ChatClient.RegisterAttempt(usernameField.text(),passwordField.text(),securityQuestionField.text(),securityAnswerField.text()))
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

  def containsNoSpecialChars(string: String) = string.matches("^[a-zA-Z\\d-_]*$")

  def isInputValid(): Boolean = {

    var errorMessage = ""

    if(!containsNoSpecialChars(usernameField.text.value)){
      errorMessage += "Username can only be [A-Z ... 0-9]!\n"
    }

    if(!containsNoSpecialChars(passwordField.text.value)){
      errorMessage += "Password can only be [A-Z ... 0-9]!\n"
    }

    if (nullChecking(usernameField.text.value)) {
      errorMessage += "Username field is empty!\n"
    }

    if (nullChecking(passwordField.text.value)) {
      errorMessage += "Password field is empty!\n"
    }

    if (nullChecking(confirmPasswordField.text.value)) {
      errorMessage += "Confirm Password field is empty!\n"
    }

    if (nullChecking(securityQuestionField.text.value)) {
      errorMessage += "Security Question field is empty!\n"
    }

    if (nullChecking(securityAnswerField.text.value)) {
      errorMessage += "Security Answer field is empty!\n"
    }

    if (usernameField.text.value.length > 64){
      errorMessage += "Username Cannot Be More Than 64 Characters\n"
    }

    if (passwordField.text.value.length > 20){
      errorMessage += "Password Cannot Be More Than 20 Characters\n"
    }

    if (securityQuestionField.text.value.length > 64){
      errorMessage += "Question Cannot Be More Than 64 Characters\n"
    }

    if (securityAnswerField.text.value.length > 64){
      errorMessage += "Answer Cannot Be More Than 64 Characters\n"
    }

    if(passwordField.text.value!= confirmPasswordField.text.value){
      errorMessage += "Passwords doesn't match!"
    }

    //DBXSS Prevention

    if (errorMessage.length() == 0) {
      return true
    }

    else {
      errorText.text = errorMessage
      return false
    }
  }







}