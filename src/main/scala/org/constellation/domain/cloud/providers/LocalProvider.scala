package org.constellation.domain.cloud.providers

import better.files.File
import cats.implicits._
import cats.data.EitherT
import cats.effect.Concurrent
import org.constellation.domain.cloud.config.Local
import org.constellation.primitives.Schema
import org.constellation.serializer.KryoSerializer

class LocalProvider[F[_]](config: Local)(implicit F: Concurrent[F]) extends CloudServiceProvider[F] {
  val name = s"local/${config.path}"

  private lazy val dir: F[File] = F.delay {
    File(config.path)
  }

  def storeSnapshot(snapshot: File, height: Long, hash: String): EitherT[F, Throwable, Unit] =
    writeFile("snapshots", s"/${height}-${hash}".some, s"${hash}-snapshot", snapshot)

  def storeSnapshotInfo(snapshotInfo: File, height: Long, hash: String): EitherT[F, Throwable, Unit] =
    writeFile("snapshots", s"/${height}-${hash}".some, s"${hash}-snapshot_info", snapshotInfo)

  def storeGenesis(genesisObservation: Schema.GenesisObservation): EitherT[F, Throwable, Unit] =
    writeClass("genesis", None, "genesis", genesisObservation)

  private def createDirectoryIfNotExists(path: File): EitherT[F, Throwable, Unit] =
    F.delay {
      path.createDirectoryIfNotExists()
    }.void.attemptT

  private def write(
    path: String,
    prefix: Option[String],
    fileName: String,
    bytes: Array[Byte]
  ): EitherT[F, Throwable, Unit] =
    dir
      .map(_ / prefix.map(path + _).getOrElse(path))
      .attemptT
      .flatMap(createDirectoryIfNotExists)
      .flatMap { _ =>
        dir
          .map(_ / prefix.map(path + _).getOrElse(path) / fileName)
          .flatMap { a =>
            F.delay {
              a.writeByteArray(bytes)
            }
          }
          .void
          .attemptT
      }

  private def writeFile(
    path: String,
    prefix: Option[String],
    filename: String,
    file: File
  ): EitherT[F, Throwable, Unit] =
    F.delay {
      file.loadBytes
    }.attemptT
      .flatMap(write(path, prefix, filename, _))

  private def writeClass[A](path: String, prefix: Option[String], fileName: String, a: A): EitherT[F, Throwable, Unit] =
    F.delay {
      KryoSerializer.serialize[A](a)
    }.attemptT.flatMap {
      write(path, prefix, fileName, _)
    }
}

object LocalProvider {
  def apply[F[_]: Concurrent](config: Local): LocalProvider[F] = new LocalProvider[F](config)
}
