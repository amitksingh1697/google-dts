package com.google.cloud.connector.data;

import java.util.Base64;

/** Util class that mainly supports stream id encoding and decoding. */
public class ResultStreamId {
  /** The collectionId for the component that represents a result set from a query preparation. */
  public static final String COLLECTION_ID_RESULTSET = "resultsets";

  /** The collectionId for the component that represents a stream within a result set. */
  public static final String COLLECTION_ID_STREAM = "streams";


  /**
   * Encodes given bytes to a result stream id string.
   *
   * @param rawBytes bytes that will be used to produce encoded stream id string.
   * @return a base64 url encoded string that represents a result stream id.
   */
  public static String encode(byte[] rawBytes) {
    return Base64.getUrlEncoder().encodeToString(rawBytes);
  }

  /**
   * Decode the given stream id string to raw bytes.
   *
   * @param base64UrlEncoded base64 url encoded result stream id.
   * @return raw bytes that was used for encoding the result stream id.
   * @throws IllegalArgumentException When base64UrlEncoded is not a valid Base64 scheme.
   */
  public static byte[] decode(String base64UrlEncoded) {
    return Base64.getUrlDecoder().decode(base64UrlEncoded);
  }
}
