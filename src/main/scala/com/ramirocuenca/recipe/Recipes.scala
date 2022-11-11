package com.ramirocuenca.recipe

import java.util.UUID

import cats.effect.Sync
import com.ramirocuenca.recipe.Recipes.{Recipe, RecipeMessage}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.{EntityDecoder, EntityEncoder}

import scala.collection.mutable.ListBuffer

trait Recipes[F[_]] {
  def create(recipe: Recipes.Recipe): F[Either[RecipeMessage, Recipe]]

  def findById(id: String): F[Either[RecipeMessage, Recipe]]
}

object Recipes {

  def impl[F[_] : Sync](): Recipes[F] = new Recipes[F] {

    val recipes = new ListBuffer[Recipe]()

    override def create(recipe: Recipe): F[Either[RecipeMessage, Recipe]] = {
      // add one element at a time to the ListBuffer
      val createdRecipe = recipe.copy(id = Option(UUID.randomUUID()))
      recipes += createdRecipe
      Sync[F].pure(Right(createdRecipe))
    }

    private def resolveId(id: String): Option[UUID] = {
      try {
        Option(UUID.fromString(id))
      } catch {
        case (_: IllegalArgumentException) => None
      }
    }

    override def findById(id: String): F[Either[RecipeMessage, Recipe]] = {
      resolveId(id) match {
        case Some(resolvedId) => Sync[F].pure {
          recipes.find(_.id.contains(resolvedId)) match {
            case Some(recipe) => Right(recipe)
            case None => Left(RecipeMessage(s"recipe did not exist with following identifier: $id"))
          }
        }
        case None => Sync[F].pure(Left(RecipeMessage(s"provided identifier was invalid: $id")))
      }

    }
  }

  case class Recipe(id: Option[UUID] = None, name: String)

  case class RecipeMessage(message: String)

  object RecipeMessage {
    implicit val recipeMessageDecoder: Decoder[RecipeMessage] = deriveDecoder[RecipeMessage]

    implicit def recipeMessageEntityDecoder[F[_] : Sync]: EntityDecoder[F, RecipeMessage] = jsonOf

    implicit val recipeMessageEncoder: Encoder[RecipeMessage] = deriveEncoder[RecipeMessage]

    implicit def recipeMessageEntityEncoder[F[_] : Sync]: EntityEncoder[F, RecipeMessage] = jsonEncoderOf

  }

  object Recipe {
    implicit val recipeDecoder: Decoder[Recipe] = deriveDecoder[Recipe]

    implicit def recipeEntityDecoder[F[_] : Sync]: EntityDecoder[F, Recipe] = jsonOf

    implicit val recipeEncoder: Encoder[Recipe] = deriveEncoder[Recipe]

    implicit def recipeEntityEncoder[F[_] : Sync]: EntityEncoder[F, Recipe] = jsonEncoderOf
  }

}
