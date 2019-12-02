package com.dwolla.codecommit.algebras

import cats._
import cats.effect._
import cats.effect.concurrent.Deferred
import cats.implicits._
import cats.scalatest._
import com.dwolla.codecommit.algebras.PullRequestMergeAlgSpec._
import com.dwolla.codecommit.merger.ValidationSpecHelpers._
import com.dwolla.codecommit.merger.model._
import com.dwolla.testutils.IOSpec
import io.circe.literal._
import org.scalatest.Matchers
import software.amazon.awssdk.services.codecommit.model.{Comment, PullRequest}

import scala.concurrent.duration._

class PullRequestMergeAlgSpec extends IOSpec with Matchers with ValidatedMatchers with EitherMatchers {

  behavior of "PullRequestMergeAlg"

  it should "work if getComment is called first" inIO {
    (for {
      alg <- PullRequestMergeAlgSpec.commentFirstAlg[IO]
      unit <- new PullRequestMergeAlgImpl[IO](alg).handleComment(prCommentEvent)
    } yield unit should be(())).timeout(2.seconds)
  }

  it should "work if getPullRequest is called first" inIO {
    (for {
      alg <- PullRequestMergeAlgSpec.prFirstAlg[IO]
      unit <- new PullRequestMergeAlgImpl[IO](alg).handleComment(prCommentEvent)
    } yield unit should be(())).timeout(2.seconds)
  }

}

object PullRequestMergeAlgSpec {
  def commentFirstAlg[F[_]: Concurrent]: F[CodeCommitAlg[F]] = Deferred[F, Unit].map { latch =>
    new CodeCommitAlg[F] {
      override def getComment(id: CommentId): F[Comment] =
        latch.complete(()) >> validComment.pure[F]

      override def getPullRequest(pullRequestId: PullRequestId): F[PullRequest] =
        latch.get >> validPR.pure[F]

      override def mergePullRequest(repositoryName: RepositoryName, pullRequestId: PullRequestId, tipCommitId: Option[CommitId]): F[Unit] =
        Applicative[F].unit
    }
  }

  def prFirstAlg[F[_]: Concurrent]: F[CodeCommitAlg[F]] = Deferred[F, Unit].map { latch =>
    new CodeCommitAlg[F] {
      override def getComment(id: CommentId): F[Comment] =
        latch.get >> validComment.pure[F]

      override def getPullRequest(pullRequestId: PullRequestId): F[PullRequest] =
        latch.complete(()) >> validPR.pure[F]

      override def mergePullRequest(repositoryName: RepositoryName, pullRequestId: PullRequestId, tipCommitId: Option[CommitId]): F[Unit] =
        Applicative[F].unit
    }
  }

  val prCommentEvent: PullRequestCommentEvent =
    json"""{
             "beforeCommitId": "98e51570918535a481ba6a24338f9a30cd264ed3",
             "inReplyTo": "164f09cc-a5db-4ee1-92b3-507cc10eb7ec:0495e3ea-5e38-49b9-9151-96d009e750cb",
             "notificationBody": "A pull request event occurred in the following AWS CodeCommit repository: codecommit-merge-on-comment. arn:aws:sts::006467937747:assumed-role/IT-Admin/bholt made a comment or replied to a comment. The comment was made on the following Pull Request: 3. For more information, go to the AWS CodeCommit console https://us-west-2.console.aws.amazon.com/codesuite/codecommit/repositories/codecommit-merge-on-comment/pull-requests/3/activity#164f09cc-a5db-4ee1-92b3-507cc10eb7ec%3A131232f8-b22f-4cd9-87a1-84f005de25d4?region=us-west-2",
             "repositoryId": "555f2590-a675-47a0-b35e-bc1c0e5b1822",
             "commentId": "164f09cc-a5db-4ee1-92b3-507cc10eb7ec:131232f8-b22f-4cd9-87a1-84f005de25d4",
             "afterCommitId": "bac94b54d10cbc6c121386f51e91e002e940fc19",
             "callerUserArn": "arn:aws:sts::006467937747:assumed-role/IT-Admin/bholt",
             "event": "commentOnPullRequestCreated",
             "pullRequestId": "3",
             "repositoryName": "codecommit-merge-on-comment"
           }""".as[PullRequestCommentEvent].toOption.get
}
