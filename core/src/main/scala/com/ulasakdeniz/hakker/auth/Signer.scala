package com.ulasakdeniz.hakker.auth

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Signer {

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
    val normalizedHeaderParams = normalizeParameters(params)
    val signingKey = s"${encode(consumerSecret)}&${oAuthTokenSecret.getOrElse("")}"
    val baseString = s"${httpMethod.toUpperCase}&${encode(uri)}&$normalizedHeaderParams"
    encode(hmacSha1(signingKey, baseString))
  }

  def normalizeParameters(params: List[(String, String)]): String = {
    val sorted: List[(String, String)] = params.sortBy(t => t)
    sorted.map(t => s"${encode(t._1)}%3D${encode(t._2)}").mkString("%26")
  }

  // intentionally left
  def hmacSha1(key: String, baseString: String): String = {
    val HmacSHA1 = "HmacSHA1"
    val secretKeySpec = new SecretKeySpec(key.getBytes, HmacSHA1)
    val mac = Mac.getInstance(HmacSHA1)
    mac.init(secretKeySpec)
    val calculatedValue = mac.doFinal(baseString.getBytes)
    val base64encoded = Base64.getEncoder.encode(calculatedValue)
    new String(base64encoded, "UTF-8")
  }
}
