package com.ulasakdeniz.auth

import com.ulasakdeniz.base.UnitSpec

class SignerUnitSpec extends UnitSpec {

  "encode" should {

    "percent-encode the given string" in new SignerUnitSpecFixture {
      val input = "http://akka.io"
      val expected = "http%3A%2F%2Fakka.io"

      TestSigner.encode(input) shouldEqual expected
    }
  }

  "normalizeEncodeParameters" should {

    "sort and encode elements of List[(String, String)]" in new SignerUnitSpecFixture {
      val expected = "a%3Da%26a%3Da"

      doReturn("a").when(signerSpy).encode("a")
      signerSpy.normalizeEncodeParameters(params) shouldEqual expected
      verify(signerSpy, times(params.size * 2)).encode("a")
    }
  }

  "hmac" should {

    "hash the signing key and base string with HmacSHA1 if a different algorithm not specified" in
      new SignerUnitSpecFixture {
        val result = TestSigner.hmac(key, baseString)
        val expected = "3nybhbi3iqa8ino29wqQcBydtNk="

        result shouldEqual expected
    }

    "hash the signing key and base string with HmacMD5 if explicitly passed" in
      new SignerUnitSpecFixture {
        val result = TestSigner.hmac(key, baseString, MD5)
        val expected = "gAcHE0Y+d0m5DC3CSRHidQ=="

        result shouldEqual expected
    }

    "throw exception if key parameter is empty" in new SignerUnitSpecFixture {
      an[IllegalArgumentException] should be thrownBy TestSigner.hmac(emptyString, baseString)
    }
  }

  "generateSignature" should {
    "create a signature by creating signing key and base string with other functions in Signer" in
      new SignerUnitSpecFixture {
        val sha1 = "3nybhbi3iqa8ino29wqQcBydtNk="
        val encodedSha1 = "3nybhbi3iqa8ino29wqQcBydtNk%3D"
        val normalizedHeaderParams = "a%3Da%26a%3Da"
        val generatedBaseString = s"POST&$encodedUri&$normalizedHeaderParams"
        val expected = baseString

        doReturn(consumerSecret).when(signerSpy).encode(consumerSecret)
        doReturn(encodedUri).when(signerSpy).encode(uri)
        doReturn(encodedSha1).when(signerSpy).encode(sha1)
        doReturn(expected).when(signerSpy).hmac(consumerSecret + "&", generatedBaseString, SHA1)
        doReturn(expected).when(signerSpy).encode(expected)

        signerSpy.generateSignature(httpMethod, uri, params, consumerSecret) shouldEqual expected
      }
  }

  trait SignerUnitSpecFixture {
    object TestSigner extends Signer
    val signerSpy = spy(TestSigner)

    val emptyString = ""
    val key = "key"
    val baseString = "The quick brown fox jumps over the lazy dog"

    val params = List("a" -> "a", "a" -> "a")
    val httpMethod = "post"
    val uri = "http://ulasakdeniz.com"
    val encodedUri = "http%3A%2F%2Fulasakdeniz.com"
    val consumerSecret = "Everyone has a secret"
    val oauthTokenSecret = baseString
  }
}
