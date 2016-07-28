package app.services

import com.redis.RedisClient
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
// TODO: change with custom EC
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object Redis extends AbstractRedis

abstract class AbstractRedis {

  private val config = ConfigFactory.load()

  val defaultPort = 6379
  val host        = Try(config.getString("redis.host")).getOrElse("localhost")
  val port        = Try(config.getInt("redis.port")).getOrElse(defaultPort)

  private val client: RedisClient = new RedisClient(host, port)

  def setOAuthTokens(tokens: Map[String, String]): Future[Boolean] = {
    val oauth_token = "oauth_token"
    val ttlSeconds  = 100
    tokens
      .get(oauth_token)
      .map(token => {
        Future {
          client.hmset(token, tokens)
          client.expire(token, ttlSeconds)
        }
      })
      .getOrElse(Future.successful(false))
  }

  def setHM(key: String, map: Map[String, String]): Future[Boolean] = {
    Future {
      client.hmset(key, map)
    }
  }

  def getHM(key: String): Future[Option[Map[String, String]]] = {
    Future {
      client.hgetall(key)
    }
  }

  def delete(key: String): Future[Option[Long]] = {
    Future {
      client.del(key)
    }
  }
}
