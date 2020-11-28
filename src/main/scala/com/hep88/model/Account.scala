package com.hep88.model

import com.hep88.util.Database

import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalikejdbc._

import scala.util.Try

class Account(userNameS: String,
              passwordS: String,
              accIDS: Int = 0
          )extends Database {

  var userName = new StringProperty(userNameS)
  val password = new StringProperty(passwordS)
  val accountID = ObjectProperty[Int](accIDS)

  //createAcc
  def createAccount() = {
      (DB autoCommit {
        implicit session =>
          sql"""
                insert into account(Username,Password) values
                (${userName.value},${password.value})
                """.updateAndReturnGeneratedKey().apply()
      })
  }


  //verify if username exists
  def isExist: Boolean = {
    DB readOnly { implicit session =>
      sql"""
            select * from account where
            Username = ${userName.value}
            """.map(rs => rs.string("Username")).single.apply()
    } match {
      case Some(x) => true
      case None => false
    }
  }

  //verify login
  def isVerify: Boolean = {
    var accountChecking: Boolean = false
    if (isExist) {
      DB readOnly { implicit session =>
        sql"""
            select * from account where
            Username = ${userName.value} AND
            Password = ${password.value}
            """.map(rs => rs.string("Password")).single.apply()
      } match {
        case Some(x) => accountChecking = true
        case None => accountChecking = false
      }
    }
    return accountChecking
  }
  
}


object Account extends Database {
  def apply(
             userNameS: String,
             passwordS: String,
             accIDS: Int
           ): Account = {
    new Account(userNameS,passwordS,accIDS)
  }

  def initializeTable() = {
    DB autoCommit { implicit session =>
      sql"""
            create table account(
            AccountID int not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
            Username varchar(64) NOT NULL,
            Password varchar(20)
            )
        """.execute.apply()
    }
  }

  /*
  def getAccount: List[Account]= {
    DB readOnly { implicit session =>
      sql"select * from account".map(rs => Account(rs.string("Name"), rs.string("Date_of_Birth"),
        rs.string("Email"), rs.string("LicenseID"), rs.string("Password"),
        rs.int("AccountID"))).list.apply()
    }
  }*/

}

