package com.dwolla.codecommit.algebras

import cats._
import cats.effect.Resource
import cats.implicits._
import com.dwolla.codecommit.merger._
import com.dwolla.codecommit.merger.model._

trait PullRequestMergeAlg[F[_]] {
  def handleComment(event: PullRequestCommentEvent): F[Unit]
}

object PullRequestMergeAlg {
  def resource[F[_] : MonadError[*[_], Throwable] : NonEmptyParallel](codeCommitAlg: CodeCommitAlg[F]): Resource[F, PullRequestMergeAlg[F]] =
    Resource.pure(new PullRequestMergeAlgImpl[F](codeCommitAlg))
}

private[algebras] class PullRequestMergeAlgImpl[F[_] : MonadError[*[_], Throwable] : NonEmptyParallel](codeCommitAlg: CodeCommitAlg[F]) extends PullRequestMergeAlg[F] {
  private def getCommentContext(event: PullRequestCommentEvent): F[CommentContext] =
    (codeCommitAlg.getComment(event.commentId), codeCommitAlg.getPullRequest(event.pullRequestId))
      .parMapN(CommentContext.apply)

  override def handleComment(event: PullRequestCommentEvent): F[Unit] =
    for {
      context@CommentContext(_, pr) <- getCommentContext(event)
      _ <- PullRequestCommentValidation.validate(context).leftMap(ValidationException).liftTo[F]
      _ <- codeCommitAlg.mergePullRequest(event.repositoryName, tagPullRequestId(pr.pullRequestId()), None)
    } yield ()
}
