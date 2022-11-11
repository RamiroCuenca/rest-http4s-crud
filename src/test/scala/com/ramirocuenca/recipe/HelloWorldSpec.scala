package com.ramirocuenca.recipe

import java.util.UUID

import cats.effect.IO
import com.example.recipeservice.Recipes.{Recipe, RecipeMessage}
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite
import org.http4s
import io.circe.syntax._
import org.http4s.circe._

/*
1. Create Testing class => RecipesSpec.
2. Create a server instance. We need to provide the routes to it.
3. Execute HTTP calls/requests. 
4. Create test cases:
    test("case description") {
      val ${scenario}Response: Response[IO] = ${HTTP request from 4)}
    }
*/

/* 1) Create Testing class */
class RecipesSpec extends CatsEffectSuite {

  /* 2) Create a server instance */
  val server: http4s.HttpApp[IO] = {
    RecipeserviceRoutes.recipeRoutes[IO](Recipes.impl[IO]()).orNotFound
  }

  /* 3) Execute HTTP calls */
  private[this] def getRecipe(id: String): Response[IO] = {
    val getRecipe: Request[IO] = Request[IO](Method.GET, uri"/recipes" / id)
    this.server.run(getRecipe).unsafeRunSync()
  }

  private[this] def createRecipe(recipe: Recipe): Response[IO] = {
    val postRecipe: Request[IO] = Request[IO](Method.POST, uri"/recipes").withEntity(recipe.asJson)
    this.server.run(postRecipe).unsafeRunSync()
  }

  /* 4) Create test cases */
  test("creating recipes") {
    val createdRecipeResponse: Response[IO] = createRecipe(Recipe(name = "foo"))
    val createdRecipe: IO[Recipe] = createdRecipeResponse.as[Recipe]
    assert(createdRecipeResponse.status == Status.Created, s"Expected: ${Status.Created}, Actual: ${createdRecipeResponse.status}")
    assertIO(createdRecipe.map(_.name), "foo")
    assertIOBoolean(createdRecipe.map(_.id.isDefined), "id was not defined")
  }

  test("retrieving recipes") {
    val createdRecipe: Recipe = createRecipe(Recipe(name = "retrieving recipes")).as[Recipe].unsafeRunSync()
    val recipeId: UUID = createdRecipe.id.getOrElse(fail("identifier was not provided"))
    val resolvedRecipeResponse = getRecipe(recipeId.toString)
    val resolvedRecipe = resolvedRecipeResponse.as[Recipe]
    assert(resolvedRecipeResponse.status == Status.Ok, s"Expected: ${Status.Ok}, Actual: ${resolvedRecipeResponse.status}")
    assertIO(resolvedRecipe.map(_.name), "foo")
    assertIOBoolean(resolvedRecipe.map(_.id.contains(recipeId)), "id did not match")
  }

  test("retrieving non-existent recipes") {
    val recipeId = UUID.randomUUID().toString
    val resolvedRecipeResponse = getRecipe(recipeId)
    val resolvedRecipe = resolvedRecipeResponse.as[RecipeMessage]
    assert(resolvedRecipeResponse.status == Status.NotFound, s"Expected: ${Status.NotFound}, Actual: ${resolvedRecipeResponse.status}")
    assertIO(resolvedRecipe.map(_.message), s"recipe did not exist with following identifier: $recipeId")
  }

  test("retrieving recipes with invalid identifiers") {
    val invalidRecipeId = "1234"
    val resolvedRecipeResponse = getRecipe(invalidRecipeId)
    val resolvedRecipe = resolvedRecipeResponse.as[RecipeMessage]
    assert(resolvedRecipeResponse.status == Status.BadRequest, s"Expected: ${Status.BadRequest}, Actual: ${resolvedRecipeResponse.status}")
    assertIO(resolvedRecipe.map(_.message), s"provided identifier was invalid: $invalidRecipeId")
  }
}