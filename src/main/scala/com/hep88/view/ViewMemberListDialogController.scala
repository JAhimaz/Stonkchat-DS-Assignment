package com.hep88.view

import javafx.{scene => jfxs}
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.Includes._
import scalafx.scene.control.{Label, ListView}
import scalafx.collections.ObservableBuffer
import scalafx.stage.Stage
import com.hep88.User



@sfxml
class ViewMemberListDialogController(
    private val listMember: ListView[User]
){
    var dialogStage : Stage = null

    def updateMemberList(x: Iterable[User]):Unit={
        listMember.items= new ObservableBuffer[User]() ++= x
    }

    def handleOk(action: ActionEvent):Unit={
        dialogStage.close()
    }



}