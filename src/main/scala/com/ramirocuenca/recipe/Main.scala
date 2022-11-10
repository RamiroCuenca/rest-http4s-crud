package com.ramirocuenca.recipe

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    RecipeServer.stream[IO].compile.drain
