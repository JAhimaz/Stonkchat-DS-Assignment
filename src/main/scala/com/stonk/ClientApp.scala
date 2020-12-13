package com.stonk
import akka.cluster.typed._
import akka.{ actor => classic }
import akka.discovery.{Discovery,Lookup, ServiceDiscovery}
import akka.discovery.ServiceDiscovery.Resolved
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.adapter._
import com.typesafe.config.ConfigFactory
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.image.Image
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scala.concurrent.Future
import scala.concurrent.duration._  
import scalafx.stage.{Modality, Stage, StageStyle}

import com.stonk.model.Account
import com.stonk.model.SubGroupActor

import javafx.{scene => jfxs}


object Client extends JFXApp {

  // ----------------------------------------

    // Fill Only These ###

    // If You're a Client (Type the Server host's public IP)
    var publicSeedNodeIP = "115.132.6.17"

    // If You're Server Host or Testing Locally, Set To True
    var isServerHost = true
    var publicServerPort = 25520

    var localPort = 2222

  // ----------------------------------------

  var loggedInUser : String = null
  var mainController : Option[com.stonk.view.MainWindowController#Controller] = None
  var loginController : Option[com.stonk.view.LogInController#Controller] = None
  var registerController : Option[com.stonk.view.RegistryController#Controller] = None
  var groupCreationController : Option[com.stonk.view.GroupCreationDialogController#Controller] = None
  var groupChatController : Option[com.stonk.view.GroupChatController#Controller] = None
  var viewMemberListController : Option[com.stonk.view.ViewMemberListDialogController#Controller] = None
  var userNameRequestController : Option[com.stonk.view.UserNameRequestController#Controller] = None
  var resetPasswordController: Option[com.stonk.view.ResetPasswordController#Controller] = None

  //code for akka system initialization
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val config = ConfigFactory.load()
  val mainSystem = akka.actor.ActorSystem("StonkChatSys", MyConfiguration.askForConfig().withFallback(config))
  val greeterMain: ActorSystem[Nothing] = mainSystem.toTyped

  val cluster = Cluster(greeterMain)
  //val greeterMain: ActorSystem[ChatClient.Command] = ActorSystem(ChatClient(), "StonkChatSys")

  //discover cluster
  val discovery: ServiceDiscovery = Discovery(mainSystem).discovery

  val userRef = mainSystem.spawn(ChatClient(), "ChatClient")

  def joinPublicSeedNode(): Unit = {
    val address = akka.actor.Address("akka", "StonkChatSys", publicSeedNodeIP , publicServerPort)
    cluster.manager ! JoinSeedNodes(List(address))
  }

  def joinLocalSeedNode(): Unit = {
      val address = akka.actor.Address("akka", "StonkChatSys", MyConfiguration.localAddress.get.getHostAddress, localPort)
      cluster.manager ! JoinSeedNodes(List(address))
  }

  if(isServerHost){
    joinLocalSeedNode()
  }else{
    joinPublicSeedNode()
  }

  userRef ! ChatClient.start

  //ScalaFX initialization code ----------------------------------------------------------------------------------------------------------
  //user interface file to read the ScalaFX

  val icon : Image = new Image(getClass().getResourceAsStream("/images/Logo.png"))

  val rootResource = getClass.getResourceAsStream("view/RootLayout.fxml")
  val loader = new FXMLLoader(null, NoDependencyResolver)
  loader.load(rootResource);
  val roots = loader.getRoot[jfxs.layout.BorderPane]
  val cssResource = getClass.getResource("view/stylesheet.css")
  roots.stylesheets = List(cssResource.toExternalForm)
  stage = new PrimaryStage() {
    title = "StonkChat | Chatting Reinvented"
    resizable = false
    scene = new Scene(){
      root = roots
    }
  }

  stage.getIcons.add(icon)

  def showMainChat()={
    val resource = getClass.getResourceAsStream("view/MainWindow.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    mainController = Option(loader.getController[com.stonk.view.MainWindowController#Controller])//object that control the interaction with the user
    mainController.get.chatClientRef = Option(userRef)
    this.roots.setCenter(roots)
    Client.mainController map (_.updateList(ChatClient.groups.toList.filter(y => ! ChatClient.unreachables.exists (x => x == y.ref.path.address))))
  }

  def showLogIn():Unit ={
    val resource = getClass.getResourceAsStream("view/LogInView.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    loginController = Option(loader.getController[com.stonk.view.LogInController#Controller])
    loginController.get.chatClientRef = Option(userRef)
    this.roots.setCenter(roots)
  }


  def showResetPassword(question: String,username: String):Unit={
    val resource = getClass.getResourceAsStream("view/ResetPassword.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    resetPasswordController = Option(loader.getController[com.stonk.view.ResetPasswordController#Controller])
    resetPasswordController.get.chatClientRef = Option(userRef)
    resetPasswordController.get.resetInitializer(username,question)
    this.roots.setCenter(roots)
  }


  def showGroupCreationDialog():Boolean ={
    val resource = getClass.getResourceAsStream("view/GroupCreationDialog.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots2 = loader.getRoot[jfxs.Parent]
    
    groupCreationController = Option(loader.getController[com.stonk.view.GroupCreationDialogController#Controller])
    groupCreationController.get.chatClientRef = Option(userRef)
    roots2.stylesheets = List(cssResource.toExternalForm)
    val dialog = new Stage() {
      initModality(Modality.APPLICATION_MODAL)
      initOwner(stage)
      scene = new Scene {
        root = roots2
      }
    }
    groupCreationController.get.dialogStage = dialog
    //groupCreationController.get.gname = gname
    dialog.showAndWait()
    groupCreationController.get.okClicked
  }

  def showUserNameRequestDialog():Boolean ={
    val resource = getClass.getResourceAsStream("view/UsernameRequestDialog.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots2 = loader.getRoot[jfxs.Parent]
    
    userNameRequestController = Option(loader.getController[com.stonk.view.UserNameRequestController#Controller])
    userNameRequestController.get.chatClientRef = Option(userRef)
    roots2.stylesheets = List(cssResource.toExternalForm)
    val dialog = new Stage() {
      initModality(Modality.APPLICATION_MODAL)
      initOwner(stage)
      scene = new Scene {
        root = roots2
      }
    }
    userNameRequestController.get.dialogStage = dialog
    dialog.showAndWait()
    userNameRequestController.get.okClicked
  }


  def showMemberListDialog(list: Iterable[User]):Unit ={
    val resource = getClass.getResourceAsStream("view/MemberListViewDialog.fxml")
    val loader = new FXMLLoader(null,NoDependencyResolver)
    loader.load(resource);
    val roots2 = loader.getRoot[jfxs.Parent]
    viewMemberListController = Option(loader.getController[com.stonk.view.ViewMemberListDialogController#Controller])
    val dialog = new Stage() {
      initModality(Modality.APPLICATION_MODAL)
      initOwner(stage)
      scene = new Scene {
        root = roots2
      }
    }
    viewMemberListController.get.dialogStage=dialog
    viewMemberListController.get.updateMemberList(list)
    dialog.showAndWait()
  }


  def showGroupChat():Unit ={
    val resource = getClass.getResourceAsStream("view/groupView.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    groupChatController = Option(loader.getController[com.stonk.view.GroupChatController#Controller])
    groupChatController.get.chatClientRef = Option(userRef)
    groupChatController.get.receiveGroupName(ChatClient.groupName)
    this.roots.setCenter(roots)
  }

  showLogIn()

  stage.onCloseRequest = handle( {
    mainSystem.terminate
  })
 
}