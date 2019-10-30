package com.dwolla.codecommit.merger

import cats.data._
import cats.implicits._
import com.dwolla.testutils.IOSpec
import org.scalatest.Matchers

class ValidationSpec extends IOSpec with Matchers {

  behavior of "Validated Kleisli"

  it should "shortcut failures" in {
    implicit class KleisliAndThen[E, A, B](k: Kleisli[Validated[E, *], A, B]) {
      def validatedAndThen[C](f: B => Validated[E, C]): Kleisli[Validated[E, *], A, C] =
        validatedAndThen(Kleisli(f))

      def validatedAndThen[C](fa: Kleisli[Validated[E, *], B, C]): Kleisli[Validated[E, *], A, C] =
        k.mapF(_.toEither).andThen(fa.mapF(_.toEither)).mapF(_.toValidated)
    }

    val either: Kleisli[Either[String, *], Int, Int] = Kleisli({
      case 1 => "one".asLeft
      case i => i.asRight
    })

    val validatorA: Int => ValidatedNel[String, Unit] = {
      case 1 => "validated A".invalidNel
      case _ => ().valid
    }

    val validatorB: Int => ValidatedNel[String, Unit] = {
      case 1 => "validated B".invalidNel
      case _ => ().valid
    }

    val kleisliValidatorA: Kleisli[ValidatedNel[String, *], Int, Unit] = Kleisli(validatorA)
    val kleisliValidatorB: Kleisli[ValidatedNel[String, *], Int, Unit] = Kleisli(validatorB)

    val combined: Kleisli[ValidatedNel[String, *], Int, Unit] = List(kleisliValidatorA, kleisliValidatorB).combineAll

    val expected: Kleisli[ValidatedNel[String, *], Int, Int] = either.mapF(_.toValidatedNel)

    val y: Kleisli[ValidatedNel[String, *], Int, Unit] = expected.validatedAndThen(combined)

    val x: Kleisli[ValidatedNel[String, *], Int, Unit] =
      expected.mapF(_.toEither).andThen(combined.mapF(_.toEither)).mapF(_.toValidated)

    x.run(1) should be("one".invalidNel)
    y.run(1) should be("one".invalidNel)
  }

  it should "accumulate errors" in {
    val either: Int => Either[String, Int] = {
      case 1 => "one".asLeft
      case i => i.asRight
    }

    val validatorA: Int => ValidatedNel[String, Unit] = {
      case 2 => "validated A".invalidNel
      case _ => ().valid
    }

    val validatorB: Int => ValidatedNel[String, Unit] = {
      case 2 => "validated B".invalidNel
      case _ => ().valid
    }

    val expected: ValidatedNel[String, Unit] =
      either(2).toValidatedNel.andThen(i => List(validatorA, validatorB).map(_ (i)).combineAll)

    expected should be("validated A".invalidNel[Unit] |+| "validated B".invalidNel)
  }

  it should "come up with a valid result" in {
    val either: Int => Either[String, Int] = {
      case 1 => "one".asLeft
      case i => i.asRight
    }

    val validatorA: Int => ValidatedNel[String, Unit] = {
      case 2 => "validated A".invalidNel
      case _ => ().valid
    }

    val validatorB: Int => ValidatedNel[String, Unit] = {
      case 2 => "validated B".invalidNel
      case _ => ().valid
    }

    val expected: ValidatedNel[String, Int] =
      either(42).toValidatedNel.andThen(i => List(validatorA, validatorB).map(_ (i)).combineAll.map(_ => i))

    expected should be(42.valid)
  }

  behavior of "tap"

  it should "pass thru the input on success" in {
    val k: Kleisli[ValidatedNel[String, *], String, String] = Kleisli((_: String) match {
      case "good" => ().validNel
      case s => s.invalidNel
    }).tap[String]

    k.run("good") should be("good".validNel)
  }

  it should "return errors on failure" in {
    val k: Kleisli[ValidatedNel[String, *], String, String] = Kleisli((_: String) match {
      case "good" => ().validNel
      case _ => "nope".invalidNel
    }).tap[String]

    k.run("bad") should be("nope".invalidNel)
  }

}
