package com.ulasakdeniz.auth.oauth1

import java.net.URLEncoder
import java.util.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Signer extends Signer

abstract class Signer {

  def encode(input: String): String = {
    // https://en.wikipedia.org/wiki/Percent-encoding#Percent-encoding_reserved_characters
    URLEncoder.encode(input, "UTF-8")
  }

  // http://oauth.net/core/1.0/#signing_process
  def generateSignature(httpMethod: String,
                        uri: String,
                        params: List[(String, String)],
                        consumerSecret: String,
                        oauthTokenSecret: Option[String] = None): String = {
    val normalizedParams = normalizeEncodeParameters(params)
    val signingKey       = s"${encode(consumerSecret)}&${oauthTokenSecret.getOrElse("")}"
    val baseString       = s"${httpMethod.toUpperCase}&${encode(uri)}&$normalizedParams"
    encode(hmac(signingKey, baseString))
  }

  def normalizeEncodeParameters(params: List[(String, String)]): String = {
    val sorted: List[(String, String)] = params.sortBy(t => t)
    sorted.map(t => s"${encode(t._1)}%3D${encode(t._2)}").mkString("%26")
  }

  def hmac(key: String, baseString: String, algorithm: MACAlgorithm = SHA1): String = {
    val secretKeySpec = new SecretKeySpec(key.getBytes, algorithm.value)
    val mac           = Mac.getInstance(algorithm.value)
    mac.init(secretKeySpec)
    val calculatedValue = mac.doFinal(baseString.getBytes)
    val base64encoded   = Base64.getEncoder.encode(calculatedValue)
    new String(base64encoded, "UTF-8")
  }
}

sealed trait MACAlgorithm {
  val value: String
}
case object SHA1 extends MACAlgorithm {
  override val value: String = "HmacSHA1"
}
case object MD5 extends MACAlgorithm {
  override val value: String = "HmacMD5"
}
