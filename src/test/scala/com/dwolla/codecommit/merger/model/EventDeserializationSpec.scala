package com.dwolla

import cats.implicits._
import com.dwolla.codecommit.merger.model._
import com.dwolla.sns.model._
import io.circe._
import io.circe.literal._
import org.scalatest._

class EventDeserializationSpec extends FlatSpec with Matchers with EitherValues {

  val snsInput =
    json"""{
             "Records": [
               {
                 "EventSource": "aws:sns",
                 "EventVersion": "1.0",
                 "EventSubscriptionArn": "arn:aws:sns:us-west-2:000000000000:CodeCommitPullRequestComments:1f05f39c-05c6-4787-a60b-89c4a4a80bae",
                 "Sns": {
                   "Type": "Notification",
                   "MessageId": "9e19450e-645d-5522-9ed4-1330f679cb70",
                   "TopicArn": "arn:aws:sns:us-west-2:000000000000:CodeCommitPullRequestComments",
                   "Subject": null,
                   "Message": "{\"account\":\"000000000000\",\"detailType\":\"CodeCommit Comment on Pull Request\",\"region\":\"us-west-2\",\"source\":\"aws.codecommit\",\"time\":\"2019-11-07T21:42:55Z\",\"notificationRuleArn\":\"arn:aws:codestar-notifications:us-west-2:000000000000:notificationrule/a1f30755-35a7-4509-8cd5-9ceacfe2b1b7\",\"detail\":{\"beforeCommitId\":\"98e51570918535a481ba6a24338f9a30cd264ed3\",\"inReplyTo\":\"164f09cc-a5db-4ee1-92b3-507cc10eb7ec:0495e3ea-5e38-49b9-9151-96d009e750cb\",\"notificationBody\":\"A pull request event occurred in the following AWS CodeCommit repository: codecommit-merge-on-comment. arn:aws:iam::123456789012:user/bholt made a comment or replied to a comment. The comment was made on the following Pull Request: 3. For more information, go to the AWS CodeCommit console https://us-west-2.console.aws.amazon.com/codesuite/codecommit/repositories/codecommit-merge-on-comment/pull-requests/3/activity#164f09cc-a5db-4ee1-92b3-507cc10eb7ec%3A131232f8-b22f-4cd9-87a1-84f005de25d4?region=us-west-2\",\"repositoryId\":\"555f2590-a675-47a0-b35e-bc1c0e5b1822\",\"commentId\":\"164f09cc-a5db-4ee1-92b3-507cc10eb7ec:131232f8-b22f-4cd9-87a1-84f005de25d4\",\"afterCommitId\":\"bac94b54d10cbc6c121386f51e91e002e940fc19\",\"callerUserArn\":\"arn:aws:iam::123456789012:user/bholt\",\"event\":\"commentOnPullRequestCreated\",\"pullRequestId\":\"3\",\"repositoryName\":\"codecommit-merge-on-comment\"},\"resources\":[\"arn:aws:codecommit:us-west-2:000000000000:codecommit-merge-on-comment\"],\"additionalAttributes\":{\"commentedLine\":null,\"resourceArn\":\"arn:aws:codecommit:us-west-2:000000000000:codecommit-merge-on-comment\",\"comments\":[{\"authorArn\":\"arn:aws:iam::123456789012:user/bholt\",\"commentText\":\"comment comment comment\"},{\"authorArn\":\"arn:aws:iam::123456789012:user/bholt\",\"commentText\":\"comment after policy change\"}],\"commentedLineNumber\":null,\"filePath\":null}}",
                   "Timestamp": "2019-11-07T21:43:00.207Z",
                   "SignatureVersion": "1",
                   "Signature": "hcpKXuncSl7DK7wOH8d8JvZLu3xfe8NH6F+CQNc6iOFIrraIuaPh4bvj6paOYGNKSAYTBcVATbbAgUR4puDhiym7CF2Rnh8XhaCwpliaWPuZjGfdcZ71OJTb78jVaUR/c36xT+EmTx/EnBhX0P1A1DQT9/xabFMBH7Kq3h4gR6145nJoRKnRlOXHXCvq9Rs+29if5dNlsQphb4ob54CGES/Kow2ZYidibZFmgOjF2A5nwU4GE9hg5q8pmxJI/NkoKv/4wAFCa1TrYnsvfqVlbha1JIAj/oMv6sie2emmPpu4zUy70ZBkt1h9giYeggsbqIY0Vbm4qUxq8Igw+FyKRA==",
                   "SigningCertUrl": "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-6aad65c2f9911b05cd53efda11f913f9.pem",
                   "UnsubscribeUrl": "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:000000000000:CodeCommitPullRequestComments:1f05f39c-05c6-4787-a60b-89c4a4a80bae",
                   "MessageAttributes": {}
                 }
               }
             ]
           }"""

  behavior of "sns wrapper deserialization"

  it should "decode the input with the message as decodable type" in {
    val expected: List[PullRequestCommentEvent] = {
      import com.dwolla.codecommit.merger.model._

      PullRequestCommentEvent(
        beforeCommitId = tagCommitId("98e51570918535a481ba6a24338f9a30cd264ed3"),
        repositoryId = tagRepositoryId("555f2590-a675-47a0-b35e-bc1c0e5b1822"),
        inReplyTo = tagCommentId("164f09cc-a5db-4ee1-92b3-507cc10eb7ec:0495e3ea-5e38-49b9-9151-96d009e750cb"),
        notificationBody = "A pull request event occurred in the following AWS CodeCommit repository: codecommit-merge-on-comment. arn:aws:iam::123456789012:user/bholt made a comment or replied to a comment. The comment was made on the following Pull Request: 3. For more information, go to the AWS CodeCommit console https://us-west-2.console.aws.amazon.com/codesuite/codecommit/repositories/codecommit-merge-on-comment/pull-requests/3/activity#164f09cc-a5db-4ee1-92b3-507cc10eb7ec%3A131232f8-b22f-4cd9-87a1-84f005de25d4?region=us-west-2",
        commentId = tagCommentId("164f09cc-a5db-4ee1-92b3-507cc10eb7ec:131232f8-b22f-4cd9-87a1-84f005de25d4"),
        afterCommitId = tagCommentId("bac94b54d10cbc6c121386f51e91e002e940fc19"),
        event = tagEvent("commentOnPullRequestCreated"),
        repositoryName = tagRepositoryName("codecommit-merge-on-comment"),
        callerUserArn = tagArn("arn:aws:iam::123456789012:user/bholt"),
        pullRequestId = tagPullRequestId("3"),
      )
    }.pure[List]

    val parsed: List[PullRequestCommentEvent] =
      messagesInRecordsTraversal[PullRequestCommentEvent](snsInput)

    parsed should be(expected)
  }

  behavior of "optics traversal"

  it should "not blow up if the json doesn't match the structure" in {
    val json = json"{}"

    messagesInRecordsTraversal[Json](json) should be(empty)
  }

  it should "not blow up if the message is unparsable" in {
    val json =
      json"""{
               "Records": [
                 {
                   "Sns": {
                     "Message": "well-nigh unparsable"
                   }
                 }
               ]
          }"""

    val output = messagesInRecordsTraversal[Json](json)
    output should be(empty)
  }

}
