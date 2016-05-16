package com.ulasakdeniz.hakker.auth

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Signer extends Signer

abstract class Signer {

  // https://en.wikipedia.org/wiki/Percent-encoding#Percent-encoding_reserved_characters
  val reservedCharEncoding: Map[Char, String] = Map(
    '!' -> "%21", '#' -> "%23", '$' -> "%24", '&' ->"%26", '\'' -> "%27",
    '(' -> "%28", ')' -> "%29", '*' -> "%2A", '+' -> "%2B", ',' -> "%2C",
    '/' -> "%2F", ':' -> "%3A", ';' -> "%3B", '=' -> "%3D", '?' -> "%3F",
    '@' -> "%40", '[' -> "%5B", ']' -> "%5D"
  )

  def encode(input: String): String = {
    input.map(char =>
      reservedCharEncoding.getOrElse(char, char)
    ).mkString
  }

  // http://oauth.net/core/1.0/#signing_process
  def generateSignature(httpMethod: String,
                        uri: String,
                        params: List[(String, String)],
                        consumerSecret: String,
                        oAuthTokenSecret: Option[String] = None): String = {
    val normalizedHeaderParams = normalizeEncodeParameters(params)
    val signingKey = s"${encode(consumerSecret)}&${oAuthTokenSecret.getOrElse("")}"
    val baseString = s"${httpMethod.toUpperCase}&${encode(uri)}&$normalizedHeaderParams"
    encode(hmac(signingKey, baseString))
  }

  def normalizeEncodeParameters(params: List[(String, String)]): String = {
    val sorted: List[(String, String)] = params.sortBy(t => t)
    sorted.map(t => s"${encode(t._1)}%3D${encode(t._2)}").mkString("%26")
  }

  def hmac(key: String, baseString: String, algorithm: MACAlgorithm = SHA1): String = {
    val secretKeySpec = new SecretKeySpec(key.getBytes, algorithm.value)
    val mac = Mac.getInstance(algorithm.value)
    mac.init(secretKeySpec)
    val calculatedValue = mac.doFinal(baseString.getBytes)
    val base64encoded = Base64.getEncoder.encode(calculatedValue)
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
