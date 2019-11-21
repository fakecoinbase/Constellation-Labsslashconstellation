package org.constellation.util.wallet
import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp, Sync}
import org.constellation.keytool.KeyStoreUtils
import scopt.OParser

object ExportDecryptedKeys extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    for {
      cliParams <- loadCliParams[IO](args)
      kp <- KeyStoreUtils
        .keyPairFromStorePath[IO](cliParams.keystore, cliParams.alias, cliParams.storepass, cliParams.keypass)
      _ <- KeyStoreUtils.storeWithFileStream[IO](cliParams.privStorePath,
                                                 KeyStoreUtils.storeKeyPemDecrypted[IO](kp.getPrivate))
      _ <- KeyStoreUtils.storeWithFileStream[IO](cliParams.pubStorePath,
                                                 KeyStoreUtils.storeKeyPemDecrypted[IO](kp.getPublic))
    } yield kp
  }.fold[ExitCode](throw _, _ => ExitCode.Success)

  def loadCliParams[F[_]: Sync](args: Seq[String]): EitherT[F, Throwable, ExportKeysDecryptedConfig] = {
    val builder = OParser.builder[ExportKeysDecryptedConfig]
    val cliParser = {
      import builder._
      OParser.sequence(
        programName("key-exporter"),
        opt[String]("keystore").required
          .action((x, c) => c.copy(keystore = x)),
        opt[String]("alias").required
          .action((x, c) => c.copy(alias = x)),
        opt[String]("storepass").required
          .action((x, c) => c.copy(storepass = x.toCharArray)),
        opt[String]("keypass").required
          .action((x, c) => c.copy(keypass = x.toCharArray))
          .required,
        opt[String]("priv_store_path").required
          .action((x, c) => c.copy(privStorePath = x)),
        opt[String]("pub_store_path").required
          .action((x, c) => c.copy(pubStorePath = x))
      )
    }
    EitherT.fromEither[F] {
      OParser
        .parse(cliParser, args, ExportKeysDecryptedConfig())
        .toRight(new RuntimeException("ExportKeysDecrypted CLI params are missing"))
    }
  }
}

case class ExportKeysDecryptedConfig(
  keystore: String = null,
  alias: String = null,
  storepass: Array[Char] = null,
  keypass: Array[Char] = null,
  privStorePath: String = null,
  pubStorePath: String = null
)
