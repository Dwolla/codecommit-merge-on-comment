package com.dwolla.codecommit.algebras

import cats.effect._
import cats.implicits._
import com.dwolla.codecommit.merger.model._
import com.dwolla.fs2aws.AwsEval._
import io.chrisdavenport.log4cats.Logger
import software.amazon.awssdk.services.codecommit._
import software.amazon.awssdk.services.codecommit.model._

trait CodeCommitAlg[F[_]] {
  def getComment(id: CommentId): F[Comment]
  def getPullRequest(pullRequestId: PullRequestId): F[PullRequest]
  def mergePullRequest(repositoryName: RepositoryName,
                       pullRequestId: PullRequestId,
                       tipCommitId: Option[CommitId],
                      ): F[Unit]
}

object CodeCommitAlg {
  private def acquireCodeCommentClient[F[_] : Sync]: F[CodeCommitAsyncClient] =
    Sync[F].delay(CodeCommitAsyncClient.builder().build())

  def resource[F[_] : ConcurrentEffect : Logger]: Resource[F, CodeCommitAlg[F]] =
    Resource.fromAutoCloseable(acquireCodeCommentClient[F])
      .map(new CodeCommitAlgImpl[F](_))
}

private[algebras] class CodeCommitAlgImpl[F[_] : ConcurrentEffect : Logger](client: CodeCommitAsyncClient) extends CodeCommitAlg[F] {
  override def getComment(id: CommentId): F[Comment] =
    Logger[F].info(s"fetching details of commit $id") >>
      eval[F](GetCommentRequest.builder().commentId(id).build())(client.getComment)(_.comment())

  override def mergePullRequest(repositoryName: RepositoryName,
                                pullRequestId: PullRequestId,
                                tipCommitId: Option[CommitId],
                               ): F[Unit] = {
    def request =
      tipCommitId.foldl {
        MergePullRequestByFastForwardRequest.builder()
          .pullRequestId(pullRequestId)
          .repositoryName(repositoryName)
      }(_ sourceCommitId _).build()

    Logger[F].info(s"merging pull request $pullRequestId in $repositoryName (maybe at commit $tipCommitId)") >>
      eval[F](request)(client.mergePullRequestByFastForward)(_ => ())
  }

  override def getPullRequest(pullRequestId: PullRequestId): F[PullRequest] =
    Logger[F].info(s"fetching details of pull request $pullRequestId") >>
      eval[F](GetPullRequestRequest.builder().pullRequestId(pullRequestId).build())(client.getPullRequest)(_.pullRequest())
}
