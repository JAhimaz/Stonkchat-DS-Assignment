package com.stonk.model

import com.stonk.util.Database

import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalikejdbc._

import scala.util.Try

class Account(userNameS: String,
              passwordS: String,
              userQuestionS: String,
              userAnswerS: String,
              accIDS: Int = 0
          )extends Database {

  var userName = new StringProperty(userNameS)
  var password = new StringProperty(passwordS)
  val userQuestion = new StringProperty(userQuestionS)
  val userAnswer = new StringProperty(userAnswerS)
  val accountID = ObjectProperty[Int](accIDS)

  //createAcc
  def createAccount() = {
      (DB autoCommit {
        implicit session =>
          sql"""
                insert into account (Username,Password,UserQuestion,UserAnswer) values
                (${userName.value},${password.value},${userQuestion.value},${userAnswer.value})
                """.updateAndReturnGeneratedKey().apply()
      })
  }
  //reset password
  
  def changePassword(): Try[Int]={
      Try(DB autoCommit {
        implicit session =>
          sql"""
          update account
          set
          Password  = ${password.value}
          where Username = ${userName.value}
          """.update.apply()
      })
  }

  def verifySecurityQA : Boolean ={
    var validity: Boolean = false
      DB readOnly { implicit session =>
        sql"""
            select * from account where
            Username = ${userName.value} AND
            UserAnswer = ${userAnswer.value}
            """.map(rs => rs.string("Password")).single.apply()
      } match {
        case Some(x) => validity = true
        case None => validity = false
      }

      return validity
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

  //getSecurityQuestion
  def obtainSecurityQ: String={
    DB readOnly { implicit session =>
      sql"""
            select * from account where
            Username = ${userName.value}
            """.map(rs => rs.string("UserQuestion")).single.apply()
    } match {
      case Some(x) => x
      case None => ""
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
             userQuestionS: String,
             userAnswerS: String,
             accIDS: Int
           ): Account = {
    new Account(userNameS,passwordS,userQuestionS,userAnswerS,accIDS)
  }

  def initializeTable() = {
    DB autoCommit { implicit session =>
      sql"""
            create table account (
            AccountID int not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
            Username varchar(64) NOT NULL,
            Password varchar(20),
            UserQuestion varchar(64),
            UserAnswer varchar(64)
            )
        """.execute.apply()
    }
  }
}

