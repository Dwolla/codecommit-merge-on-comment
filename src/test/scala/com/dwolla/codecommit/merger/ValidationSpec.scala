package com.dwolla.codecommit.merger

import cats.data._
import cats.implicits._
import cats.scalatest._
import com.dwolla.codecommit.merger.model._
import com.dwolla.codecommit.merger.PullRequestCommentValidation._
import com.dwolla.codecommit.merger.ValidationSpecHelpers._
import org.scalatest.{FlatSpec, Matchers}
import software.amazon.awssdk.services.codecommit.model.{Comment, PullRequest}

class ValidationSpec extends FlatSpec with Matchers with ValidatedMatchers with EitherMatchers {

  behavior of "author validation"

  it should "reject strings that don't match the regexes" in {
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment(""), pr(""))) should (haveInvalid("The pull request author `` cannot be interpreted as an IAM user or assumed role") and haveInvalid("The comment author `` cannot be interpreted as an IAM user or assumed role"))
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment("arn:aws:iam:::user/bholt"), pr(""))) should haveInvalid("The pull request author `` cannot be interpreted as an IAM user or assumed role")
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment(""), pr("arn:aws:iam:::user/bholt"))) should haveInvalid("The comment author `` cannot be interpreted as an IAM user or assumed role")
  }

  it should "reject assumed roles" in {
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment("arn:aws:sts:::assumed-role/super-admin/hacker"), pr("arn:aws:sts:::assumed-role/developer/session"))) should (haveInvalid("Assumed role `developer` (via session `session`) is not allowed to be a pull request author") and haveInvalid("Assumed role `super-admin` (via session `hacker`) is not allowed to be a comment author"))
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment("arn:aws:iam::123456789012:user/bholt"), pr("arn:aws:sts:us-east-1:123456789012:assumed-role/super-admin/hacker"))) should haveInvalid("Assumed role `super-admin` (via session `hacker`) is not allowed to be a pull request author")
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment("arn:aws:sts:::assumed-role/developer/session"), pr("arn:aws:iam::123456789012:user/bholt"))) should haveInvalid("Assumed role `developer` (via session `session`) is not allowed to be a comment author")
  }

  it should "reject when the same IAM user is the author of both the pull request and comment" in {
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment("arn:aws:iam::123456789012:user/bholt"), pr("arn:aws:iam::123456789012:user/bholt"))) should haveInvalid("Pull request author cannot approve their own PR")
  }

  it should "pass when different IAM users author the pull request and comment" in {
    validateCommenterIsNotAuthorViaRoleAssumption(CommentContext(comment("arn:aws:iam::123456789012:user/bholt"), pr("arn:aws:iam::123456789012:user/not-bholt"))) should beValid(())
  }

  behavior of "comment validation"

  it should "reject comments that include the trigger words as part of a larger comment" in {
    isApproval(Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("I would say approved, but this doesn't lgtm").build()) should beLeft("Comment is not an approval".pure[NonEmptyList])
  }

  it should """accept "lgtm" comments""" in {
    isApproval(Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("lgtm").build()) should beRight(())
    isApproval(Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("LGTM").build()) should beRight(())
    isApproval(Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("lgTM").build()) should beRight(())
  }

  it should """accept "approved" comments""" in {
    isApproval(Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("approved").build()) should beRight(())
    isApproval(Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("APPROVED").build()) should beRight(())
    isApproval(Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("Approved").build()) should beRight(())
  }

  behavior of "combined comment and PR validation"

  it should "pass when different IAM users author the pull request and comment, and the comment is an approval" in {
    val input = comment("arn:aws:iam::123456789012:user/bholt")
    validate(CommentContext(input, pr("arn:aws:iam::123456789012:user/not-bholt"))) should beRight(input)
  }
}

object ValidationSpecHelpers {
  def validComment: Comment =
    Comment.builder().authorArn("arn:aws:iam::123456789012:user/reviewer").content("lgtm").build()

  def validPR: PullRequest =
    pr("arn:aws:iam:::user/developer")

  def comment(arn: String): Comment = Comment.builder().authorArn(arn).content("lgtm").build()
  def pr(arn: String): PullRequest = PullRequest.builder().authorArn(arn).build()
}
