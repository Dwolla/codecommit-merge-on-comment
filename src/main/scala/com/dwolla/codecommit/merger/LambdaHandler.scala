package com.dwolla.codecommit.merger

import cats.effect._
import cats.implicits._
import com.dwolla.lambda.IOLambda
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe._

class LambdaHandler(printer: Printer) extends IOLambda[Json, Unit](printer) {
  def this() = this(Printer.noSpaces)

  private implicit val logger: Logger[IO] = Slf4jLogger.getLoggerFromName[IO]("LambdaLogger")

  override def handleRequest(blocker: Blocker)
                            (input: Json): IO[Option[Unit]] =
    None.pure[IO]
}
