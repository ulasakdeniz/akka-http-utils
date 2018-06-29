package com.ulasakdeniz.auth.oauth1

/**
  * Parameters required by oauth protocol.
  *
  * @see https://oauth.net/core/1.0/#request_urls
  *
  * @param consumerKey      Consumer key defined by oauth_consumer_key
  * @param consumerSecret   Consumer secret
  * @param requestTokenUri  The URL used to obtain an unauthorized Request Token.
  * @param accessTokenUri   The URL used to exchange the User-authorized Request Token for an Access Token.
  * @param authorizationUri The URL used to obtain User authorization for Consumer access.
  */
final case class OAuthParams(
    consumerKey: String,
    consumerSecret: String,
    requestTokenUri: String,
    accessTokenUri: String,
    authorizationUri: String)
