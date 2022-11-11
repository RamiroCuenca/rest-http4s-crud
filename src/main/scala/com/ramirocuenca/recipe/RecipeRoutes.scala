package com.ramirocuenca.recipe

import cats.effect.Sync
import cats.implicits._
import com.ramirocuenca.recipe.Recipes.{Recipe, RecipeMessage}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

/*
1. Create an object (singleton) for storing the routes. => RecipeRoutes
2. Create a method and define the routes inside. => recipeRoutes
2.1. Create an instance of http4s dsl (provide http methods).
2.2. Define the routes path and develop it logic.
*/

/* 1) Create an object (singleton) for storing the routes */
object RecipeRoutes {

  /* 2) Create a method and define the routes inside */
  def recipeRoutes[F[_] : Sync](recipes: Recipes[F]): HttpRoutes[F] = {
    /* 2.1) Create an instance of http4s dsl (provide http methods) */
    val dsl = new Http4sDsl[F] {}
    import dsl._

    /* 2.2) Define the routes path and develop it logic */
    HttpRoutes.of[F] {
      case request @ POST -> Root / "recipes" =>
        for {
          recipe <- request.as[Recipe] // Decode using EntityDecoder
          createdRecipeE <- recipes.create(recipe)
          resp <- createdRecipeE match {
            case Left(message) => BadRequest(message)
            case Right(createdRecipe) => Created(createdRecipe)
          }
        } yield resp

      case GET -> Root / "recipes" / id =>
        for {
          resolvedRecipeO <- recipes.findById(id)
          resp <- resolvedRecipeO match {
            case Right(resolvedRecipe) => Ok(resolvedRecipe)
            case Left(recipeMessage@RecipeMessage(message)) if message.startsWith("recipe did not exist with following identifier") => NotFound(recipeMessage)
            case Left(recipeMessage) => BadRequest(recipeMessage)
          }
        } yield {
          resp
        }
    }
  }
}
