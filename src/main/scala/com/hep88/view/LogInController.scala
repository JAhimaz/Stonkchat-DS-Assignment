package com.hep88.view

import akka.actor.typed.ActorRef
import javafx.{scene => jfxs}
import com.hep88.Client
import com.hep88.ChatClient

import scalafx.event.ActionEvent
import scalafx.scene.control.{Alert, TextField}
import scalafx.stage.Stage
import scalafxml.core.macros.sfxml
import scalafxml.core.{FXMLLoader, NoDependencyResolver}

@sfxml
class LogInController (
                        private val usernameField : TextField,
                        private val passwordField: TextField
                      ){

  var dialogStage: Stage = null

  var chatClientRef: Option[ActorRef[ChatClient.Command]] = None

  

  var username = usernameField.text
  var password = passwordField.text

  def logIn(action: ActionEvent): Unit = {
    if (isInputValid) {
        chatClientRef map (_ ! ChatClient.LogInAttempt(usernameField.text(),passwordField.text()))
    }
  }


  def successfulLogin():Unit={
    val alert = new Alert(Alert.AlertType.Information){
    initOwner(dialogStage)
    title = "Welcome"
    headerText = "Greetings"
    contentText= "Welcome back."
    }.showAndWait()
    Client.loggedInUser = usernameField.text()
    Client.showMainChat()
  }

  def failedLogin():Unit={
    val alert = new Alert(Alert.AlertType.Warning) {
    initOwner(dialogStage)
    title = "Failed to Log In"
    headerText = "Data Invalid"
    contentText = "Please insert the correct email and password."
    }.showAndWait()
  }

  /*
  def handleLogIn(account: Account): Unit = {
    if (isInputValid) {

      account.userName <== usernameField.text
      account.password <== passwordField.text

      logInClicked = true
    }
  }*/

  def nullChecking(x: String) = x == null || x.length == 0

  def isInputValid(): Boolean = {
    var errorMessage = ""
    if (nullChecking(usernameField.text.value)) {
      errorMessage += "Username Field is empty!!\n"
    }
    if (nullChecking(passwordField.text.value.toString)) {
      errorMessage += "Password Field is empty!\n"
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
    
    def showRegistry(action: ActionEvent) = {
    val resource = getClass.getResourceAsStream("/com/hep88/view/RegisterView.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots2 = loader.getRoot[jfxs.layout.AnchorPane]
    Client.registerController = Option(loader.getController[com.hep88.view.RegistryController#Controller])
    Client.registerController.get.chatClientRef = Option(Client.userRef)
    Client.roots.setCenter(roots2)
    }

  /*
  def close(action: ActionEvent): Unit = {
    MainApp.close()
  }*/

}
