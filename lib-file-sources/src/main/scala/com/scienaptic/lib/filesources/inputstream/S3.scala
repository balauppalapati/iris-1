package com.scienaptic.lib.filesources.inputstream

import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{ GetObjectRequest, S3ObjectInputStream }
import com.scienaptic.lib.common.error.ErrorTypes.ErrorMap
import com.scienaptic.lib.common.error.Exceptions
import io.vertx.scala.core.http.HttpServerRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

final case class S3(bucketName: String, accessKeyId: String, secretKeyId: String, keyName: String)
    extends InputStreamConfig {

  /**
    * Get the file input stream for file on S3 file system.
    * Exception handled by [[getInputStreamErrors]]
    *
    * @return a [[scala.concurrent.Future]] enclosing InputStream
    */
  def getInputStreamFuture: Future[S3ObjectInputStream] = Future {
    val credentials = new BasicAWSCredentials(accessKeyId, secretKeyId)
    val s3Client = AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withRegion("us-east-1")
      .withForceGlobalBucketAccessEnabled(true)
      .build
    val clientObject = s3Client.getObject(new GetObjectRequest(bucketName, keyName))
    clientObject.getObjectContent
  }

  /**
    * Handle exceptions caused by [[getInputStreamFuture]]
    *
    * @param e Exception from [[getInputStreamFuture]]
    * @return Map of errors with key pointing to the request params.
    */
  def getInputStreamErrors(e: Throwable): ErrorMap = e match {
    // Unknown exception
    case NonFatal(_) => Exceptions.unknownException(e)
  }
}

object S3 extends InputStreamConfigCompanion {

  // Keys used in request
  private object K {
    final val BucketName: String  = "bucketName"
    final val AccessKeyId: String = "accessKeyId"
    final val SecretKeyId: String = "secretKeyId"
    final val KeyName: String     = "keyName"
  }

  def fromRequest(req: HttpServerRequest): S3 = {
    val r           = req.getParam _
    val bucketName  = r(K.BucketName)
    val accessKeyId = r(K.AccessKeyId)
    val secretKeyId = r(K.SecretKeyId)
    val keyName     = r(K.KeyName)
    S3(bucketName.get, accessKeyId.get, secretKeyId.get, keyName.get)
  }
}
