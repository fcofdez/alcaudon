package org.alcaudon.runtime

import java.net.URL
import java.util.Date

import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import org.alcaudon.runtime.BlobLocation.AWSInformation

import scala.concurrent.duration._

object ObjectStorageUtils {

  def sign(bucketName: String, objectKey: String)(
      implicit awsInfo: AWSInformation): URL = {

    val s3client = AmazonS3ClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(awsInfo.credentials))
      .withRegion(awsInfo.region)
      .build()

    val expiration = new Date
    var milliSeconds = expiration.getTime
    milliSeconds += 1.hour.toMillis

    expiration.setTime(milliSeconds)

    val generatePresignedUrlRequest =
      new GeneratePresignedUrlRequest(bucketName, objectKey)
    generatePresignedUrlRequest.setMethod(HttpMethod.PUT)
    generatePresignedUrlRequest.setExpiration(expiration)

    s3client.generatePresignedUrl(generatePresignedUrlRequest)
  }
}
