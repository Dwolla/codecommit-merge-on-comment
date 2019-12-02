package com.dwolla.codecommit.merger

import cats.data._
import cats.implicits._
import com.dwolla.codecommit.merger.model._
import software.amazon.awssdk.services.codecommit.model._

import scala.language.reflectiveCalls
import scala.util.matching.Regex

object PullRequestCommentValidation {
  private def predicateValidate(pred: Boolean, errorMessage: String): ValidatedNel[String, Unit] =
    if (pred) ().validNel else errorMessage.invalidNel

  private val approvalRegex: Regex = """(?i)^lgtm|approved$""".r
  private val iamUserRegex: Regex = """arn:aws:iam:[^:]*:[^:]*:user/(.+)""".r("username")
  private val assumedRoleRegex: Regex = """arn:aws:sts:[^:]*:[^:]*:assumed-role/([^/]+)/(.+)""".r("roleName", "session")

  private def parseAuthorArn[A <: { def authorArn(): String }](a: A, context: String): ValidatedNel[String, Username] = {
    val parseAuthor: String => ValidatedNel[String, Username] = {
      case assumedRoleRegex(roleName, session) => s"Assumed role `$roleName` (via session `$session`) is not allowed to be a $context author".invalidNel
      case iamUserRegex(username) => tagUsername(username).validNel
      case other => s"The $context author `$other` cannot be interpreted as an IAM user or assumed role".invalidNel
    }

    parseAuthor(a.authorArn())
  }

  val isApproval: Kleisli[EitherNel[String, *], Comment, Unit] =
    Kleisli((s: String) => predicateValidate(approvalRegex.matches(s), "Comment is not an approval").toEither)
      .local[Comment](_.content)

  val validateCommenterIsNotAuthorViaRoleAssumption: Kleisli[ValidatedNel[String, *], CommentContext, Unit] = Kleisli { context =>
    val prUser = parseAuthorArn(context.pr, "pull request")
    val commentUser = parseAuthorArn(context.comment, "comment")

    (prUser, commentUser)
      .mapN(_ != _)
      .andThen(predicateValidate(_, "Pull request author cannot approve their own PR"))
  }

  val validate: Kleisli[EitherNel[String, *], CommentContext, Comment] = {
    val approvalCommentValidations: Kleisli[ValidatedNel[String, *], CommentContext, Unit] =
      List(
        validateCommenterIsNotAuthorViaRoleAssumption,
      ).combineAll

    for {
      CommentContext(comment, _) <- isApproval.local[CommentContext](_.comment).tap[CommentContext]
      _ <- approvalCommentValidations.mapF(_.toEither)
    } yield comment
  }

}
