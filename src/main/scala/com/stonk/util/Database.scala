package com.stonk.util

import java.sql.DriverManager

import com.stonk.{Server}
import com.stonk.model.{Account}
import scalikejdbc._



import scala.util.{Failure, Success, Try}



trait Database {
  val derbyDriverClassname = "org.apache.derby.jdbc.EmbeddedDriver"
  val dbURL = "jdbc:derby:chatDB;create=true;";
  val dbURLdestroy = "jdbc:derby:chatDB;shutdown=true;"
  // initialize JDBC driver & connection pool
  

  //var isServer=false

  Class.forName(derbyDriverClassname)

  ConnectionPool.singleton(dbURL, "me", "mine")

  
  // ad-hoc session provider on the REPL
  implicit val session = AutoSession

}


object Database extends Database{
  def setupDB() = {

    if (!hasAccountDBInitialize) {
      Account.initializeTable()
    }

  }

  def shutdown(): Unit = {
    System.out.println("Shutting down.. DB and Network Server Control..")
    val dbresult = Try(DriverManager.getConnection(dbURLdestroy))

    dbresult match {
      case Success(x) =>
      case Failure(exception) =>
        println("DB shutdown successfully")
        exception.printStackTrace()
    }
  }


  def hasAccountDBInitialize : Boolean = {
    DB getTable "account" match {
      case Some(x) => true
      case None => false
    }
  }

}


