package com.adewusi.roundup

import cats.effect.Sync
import cats.syntax.all._

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.UUID

trait GoalCache[F[_]] {
  def load: F[Option[UUID]]
  def save(uuid: UUID): F[Unit]
}

class FileGoalCache[F[_]: Sync](filePath: String) extends GoalCache[F] {

  private val path = Paths.get(filePath)

  override def load: F[Option[UUID]] =
    Sync[F]
      .blocking {
        if (Files.exists(path)) {
          val contents = new String(Files.readAllBytes(path)).trim
          if (contents.isEmpty) None else Some(UUID.fromString(contents))
        } else {
          None
        }
      }
      .handleError(_ => None)

  override def save(uuid: UUID): F[Unit] =
    Sync[F].blocking {
      Files.write(
        path,
        uuid.toString.getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      ()
    }
}
