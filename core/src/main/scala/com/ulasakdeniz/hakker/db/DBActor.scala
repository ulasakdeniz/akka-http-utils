package com.ulasakdeniz.hakker.db

import akka.persistence.{PersistentActor, RecoveryCompleted}

class DBActor extends PersistentActor {
  override def persistenceId: String = ???

  override def receiveRecover: Receive = {
    case RecoveryCompleted => {
      ???
    }
  }

  override def receiveCommand: Receive = ???
}