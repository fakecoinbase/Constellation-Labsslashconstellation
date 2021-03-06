package org.constellation.infrastructure.p2p.client

import cats.effect.{Concurrent, ContextShift}
import org.constellation.infrastructure.p2p.PeerResponse
import org.constellation.infrastructure.p2p.PeerResponse.PeerResponse
import org.http4s.client.Client
import org.constellation.domain.p2p.client.SoeClientAlgebra
import org.constellation.primitives.Schema.SignedObservationEdge
import org.constellation.session.SessionTokenService
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.Method._
import scala.language.reflectiveCalls

class SoeClientInterpreter[F[_]: Concurrent: ContextShift](client: Client[F], sessionTokenService: SessionTokenService[F]) extends SoeClientAlgebra[F] {

  def getSoe(hash: String): PeerResponse[F, Option[SignedObservationEdge]] =
    PeerResponse[F, Option[SignedObservationEdge]](s"soe/$hash")(client, sessionTokenService)

}

object SoeClientInterpreter {

  def apply[F[_]: Concurrent: ContextShift](client: Client[F], sessionTokenService: SessionTokenService[F]): SoeClientInterpreter[F] =
    new SoeClientInterpreter[F](client, sessionTokenService)
}
