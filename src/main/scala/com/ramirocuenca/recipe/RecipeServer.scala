package com.ramirocuenca.recipe

//import cats.effect.{ConcurrentEffect, Timer}
import cats.effect._
import fs2.Stream
import org.http4s.implicits.*
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object RecipeServer {

  def stream[F[_] : ConcurrentEffect](implicit T: Timer[F]): Stream[F, Nothing] = {
    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract a segments not checked
    // in the underlying routes.
    val httpApp = (
      RecipeRoutes.recipeRoutes[F](Recipes.impl[F]())
      ).orNotFound
    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    for {
      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
