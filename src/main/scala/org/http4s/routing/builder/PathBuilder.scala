package org.http4s
package routing
package builder

import cats.Show
import cats.instances.string._
import cats.syntax.semigroup._
import org.http4s.dsl.impl.{/:, Path}
import part.PathPart
import scala.language.reflectiveCalls
import scala.reflect.runtime.universe.TypeTag

trait PathBuilder { self: Route =>
  protected type ExtractPath[A] = { def unapply(s: String): Option[A] }

  private def nextPath[PP, P, A: TypeTag](
    name: Either[String, String],
    getParams: P => Params,
    mkParams0: (PP, QueryStringParams) => P,
    mkPathPart: P => PathPart,
    extract: ExtractPath[A],
    mkNewParams: (PathParams, A) => PP,
    paramTpe: Option[TypeTag[_]]
  ): Route.Aux[PP, QueryStringParams, P] = new Route {
    type PathParams = PP
    type QueryStringParams = self.QueryStringParams
    type Params = P

    protected def mkParams(pp: PP, qp: QueryStringParams): P = mkParams0(pp, qp)

    lazy val show = self.show |+| Route.shownPath[A](name)

    def pathParts(params: P) = self.pathParts(getParams(params)) :+ mkPathPart(params)
    def queryStringParts(params: P) = self.queryStringParts(getParams(params))

    lazy val paramTpes = self.paramTpes ++ paramTpe

    protected def matchPath(path: Path): Option[(Path, PP)] =
      self.matchPath(path).flatMap { case (p, ps) => p match {
        case extract(a) /: rest => Some((rest, mkNewParams(ps, a)))
        case _ => None
      }}

    protected def matchQueryString(params: Map[String, collection.Seq[String]]): Option[(Map[String, collection.Seq[String]], QueryStringParams)] =
      self.matchQueryString(params)
  }

  def /(s: String): Route.Aux[PathParams, QueryStringParams, Params] =
    nextPath[PathParams, Params, String](
      Left(s),
      identity _,
      self.mkParams(_, _),
      _ => PathPart.inst(s),
      new { def unapply(x: String): Option[String] = Some(x).filter(_ == s) },
      (pp, _) => pp,
      None)

  def /[V: Show](t: (String, ExtractPath[V]))(implicit s: Show[V], tt: TypeTag[V]): Route.Aux[(PathParams, V), QueryStringParams, (Params, V)] =
    nextPath[(PathParams, V), (Params, V), V](
      Right(t._1),
      _._1,
      (pp, qp) => (self.mkParams(pp._1, qp), pp._2),
      p => PathPart.inst((t._1, p._2))(Show.show(t => s.show(t._2))),
      t._2,
      (_, _),
      Some(tt))
}