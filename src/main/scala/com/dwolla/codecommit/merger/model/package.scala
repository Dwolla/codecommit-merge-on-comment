package com.dwolla.codecommit.merger

import cats.data._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import shapeless.tag
import shapeless.tag.@@
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.codecommit.model.{Comment, PullRequest}

package object model {
  type Version = String @@ VersionTag
  type DetailType = String @@ DetailTypeTag
  type EventSource = String @@ EventSourceTag
  type Account = String @@ AccountTag
  type Arn = String @@ ArnTag
  type CommitId = String @@ CommitIdTag
  type RepositoryId = String @@ RepositoryIdTag
  type CommentId = String @@ CommentIdTag
  type Event = String @@ EventTag
  type RepositoryName = String @@ RepositoryNameTag
  type PullRequestId = String @@ PullRequestIdTag
  type Username = String @@ UsernameTag
  type IamRoleName = String @@ IamRoleNameTag

  val tagVersion: String => Version = tag[VersionTag][String]
  val tagDetailType: String => DetailType = tag[DetailTypeTag][String]
  val tagEventSource: String => EventSource = tag[EventSourceTag][String]
  val tagAccount: String => Account = tag[AccountTag][String]
  val tagArn: String => Arn = tag[ArnTag][String]
  val tagCommitId: String => CommitId = tag[CommitIdTag][String]
  val tagRepositoryId: String => RepositoryId = tag[RepositoryIdTag][String]
  val tagCommentId: String => CommentId = tag[CommentIdTag][String]
  val tagEvent: String => Event = tag[EventTag][String]
  val tagRepositoryName: String => RepositoryName = tag[RepositoryNameTag][String]
  val tagPullRequestId: String => PullRequestId = tag[PullRequestIdTag][String]

  val tagUsername: String => Username = tag[UsernameTag][String]
  val tagIamRoleName: String => IamRoleName = tag[IamRoleNameTag][String]

  implicit def taggedStringEncoder[T]: Encoder[String @@ T] = Encoder[String].contramap(identity)
  implicit def taggedStringDecoder[T]: Decoder[String @@ T] = Decoder[String].map(tag[T][String])
  implicit def regionDecoder: Decoder[Region] = Decoder[String].map(Region.of)
  implicit def regionEncoder: Encoder[Region] = Encoder[String].contramap(_.id())

}

package model {
  case class PullRequestCommentEvent(beforeCommitId: CommitId,
                                     repositoryId: RepositoryId,
                                     inReplyTo: CommentId,
                                     notificationBody: String,
                                     commentId: CommentId,
                                     afterCommitId: CommentId,
                                     event: Event,
                                     repositoryName: RepositoryName,
                                     callerUserArn: Arn,
                                     pullRequestId: PullRequestId,
                                    )

  object PullRequestCommentEvent {
    implicit val pullRequestCommentEventDecoder: Decoder[PullRequestCommentEvent] = deriveDecoder
    implicit val pullRequestCommentEventEncoder: Encoder[PullRequestCommentEvent] = deriveEncoder
  }

  case class CommentContext(comment: Comment, pr: PullRequest)

  case class ValidationException(errors: NonEmptyList[String]) extends RuntimeException(errors.mkString_(" * ", "\n * ", ""), null, true, false)

  trait VersionTag
  trait DetailTypeTag
  trait EventSourceTag
  trait AccountTag
  trait ArnTag

  trait CommitIdTag
  trait RepositoryIdTag
  trait CommentIdTag
  trait EventTag
  trait RepositoryNameTag
  trait PullRequestIdTag

  trait UsernameTag
  trait IamRoleNameTag
}
