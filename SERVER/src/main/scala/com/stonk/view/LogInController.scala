package com.stonk.view

import akka.actor.typed.ActorRef
import javafx.{scene => jfxs}
import com.stonk.Client
import com.stonk.ChatClient

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.stage.Stage
import scalafxml.core.macros.sfxml
import scalafxml.core.{FXMLLoader, NoDependencyResolver}

import scalafx.scene.control.{Alert, TextField, Label}
import scalafx.scene.input.MouseEvent
import scalafx.scene.image.{Image, ImageView, WritableImage}

@sfxml
class LogInController (
  
    private val usernameField : TextField,
    private val passwordField : TextField,
    private val loginLogo : ImageView,
    private val errorText : Label,

  ){

  var dialogStage : Stage = null

  var chatClientRef : Option[ActorRef[ChatClient.Command]] = None

  var username = usernameField.text
  var password = passwordField.text

  val logoImage : Image = new Image(getClass().getResourceAsStream("/images/Logo.png"))
  loginLogo.setImage(logoImage)

  errorText.text = ""

  def logIn(action: ActionEvent): Unit = {
    if (isInputValid) {
        chatClientRef map (_ ! ChatClient.LogInAttempt(usernameField.text(),passwordField.text()))
    }
  }

  def successfulLogin() : Unit = {
    Client.loggedInUser = usernameField.text()
    Client.showMainChat()
  }

  def failedLogin(errorCode : Int) : Unit = {
    if(errorCode == 1){
      errorText.text = "Login Failed. Account Not Found"
    }

    if(errorCode == 2){
      errorText.text = "Account Already Logged In"
    }
  }

  def nullChecking(x: String) = x == null || x.length == 0

  def isInputValid(): Boolean = {
    var errorMessage = ""
    if (nullChecking(usernameField.text.value)) {
      errorMessage += "Empty Username. "
    }
    if (nullChecking(passwordField.text.value.toString)) {
      errorMessage += "Empty Password. "
    }

    if (errorMessage.length() == 0) {
      return true
    }
    else {
      errorText.text = errorMessage
      return false
    }

  }
    
  def showRegistry(event : MouseEvent) = {
    val resource = getClass.getResourceAsStream("/com/stonk/view/RegisterView.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots2 = loader.getRoot[jfxs.layout.AnchorPane]
    Client.registerController = Option(loader.getController[com.stonk.view.RegistryController#Controller])
    Client.registerController.get.chatClientRef = Option(Client.userRef)
    Client.roots.setCenter(roots2)
  }

}
