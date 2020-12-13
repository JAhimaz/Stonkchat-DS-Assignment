package com.hep88
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
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scala.concurrent.Future
import scala.concurrent.duration._  

import com.hep88.model.Account

import javafx.{scene => jfxs}


object Client extends JFXApp {

  var loggedInUser : String = null

  var mainController: Option[com.hep88.view.MainWindowController#Controller] = None

  var loginController: Option[com.hep88.view.LogInController#Controller] = None

  var registerController: Option[com.hep88.view.RegistryController#Controller] = None

  

    //code for akka system initialization
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val config = ConfigFactory.load()
  val mainSystem = akka.actor.ActorSystem("HelloSystem", MyConfiguration.askForConfig().withFallback(config))
  val greeterMain: ActorSystem[Nothing] = mainSystem.toTyped

  val cluster = Cluster(greeterMain)
  //val greeterMain: ActorSystem[ChatClient.Command] = ActorSystem(ChatClient(), "HelloSystem")

  //discover cluster
  val discovery: ServiceDiscovery = Discovery(mainSystem).discovery

  val userRef = mainSystem.spawn(ChatClient(), "ChatClient")

  def joinPublicSeedNode(): Unit = {
    //targetHost = their PUBLIC IP
    //targetPort = server port
    val address = akka.actor.Address("akka", "HelloSystem", "175.143.214.226" , 25520)
    cluster.manager ! JoinSeedNodes(List(address))
  }

  def joinLocalSeedNode(): Unit = {
      val address = akka.actor.Address("akka", "HelloSystem", MyConfiguration.localAddress.get.getHostAddress, 2222)
      cluster.manager ! JoinSeedNodes(List(address))
  }

  joinLocalSeedNode()
  userRef ! ChatClient.start

  //scalafx initialization code
  //user interface file to read the scalaFX
  val rootResource = getClass.getResourceAsStream("view/RootLayout.fxml")
  val loader = new FXMLLoader(null, NoDependencyResolver)
  loader.load(rootResource);
  val roots = loader.getRoot[jfxs.layout.BorderPane]
  stage = new PrimaryStage() {
    scene = new Scene(){
      root = roots
    }
  }


  def showMainChat()={
    val resource = getClass.getResourceAsStream("view/MainWindow.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    mainController = Option(loader.getController[com.hep88.view.MainWindowController#Controller])//object that control the interaction with the user
    mainController.get.chatClientRef = Option(userRef)
    this.roots.setCenter(roots)
  }




  def showLogIn()={
    val resource = getClass.getResourceAsStream("view/LogInView.fxml")
    val loader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource);
    val roots = loader.getRoot[jfxs.layout.AnchorPane]
    loginController = Option(loader.getController[com.hep88.view.LogInController#Controller])//object that control the interaction with the user
    loginController.get.chatClientRef = Option(userRef)
    this.roots.setCenter(roots)
  }



  showLogIn()


  stage.onCloseRequest = handle( {
    mainSystem.terminate
  })
 
}