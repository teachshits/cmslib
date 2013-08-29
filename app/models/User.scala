package models

import be.objectify.deadbolt.core.models.{Role, Permission, Subject}
import java.util.{List => JList}
import scala.collection.JavaConverters._
import reactivemongo.bson._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

/**
 * CMS User.
 * @param id MongoDB id
 * @param name user name
 * @param salt password salt
 * @param password password hashed with bcrypt using salt
 * @param email email address
 * @param roles list of roles this user has
 */
case class User( id: Option[BSONObjectID],
            name: String,
            password: String,
            email: String,
            roles: Seq[String]
          ) extends Subject {

  class UserRole(name: String) extends Role {
    def getName: String = name
  }

  def getRoles: JList[_ <: Role] = roles.map(new UserRole(_)).asJava

  def getPermissions: JList[_ <: Permission] = ???

  def getIdentifier: String = id.map(_.stringify).getOrElse(sys.error("User without BSONObjectID used for authorization"))
}

object User {
  implicit object UserBSONReader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument): User =
      User(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAsTry[String]("name").get,
        doc.getAsTry[String]("password").get,
        doc.getAsTry[String]("email").get,
        doc.getAsTry[Seq[String]]("roles").get)
  }
  implicit object UserBSONWriter extends BSONDocumentWriter[User] {
    def write(user: User): BSONDocument =
      BSONDocument(
        "_id" -> user.id.getOrElse(BSONObjectID.generate),
        "name" -> user.name,
        "password" -> user.password,
        "email" -> user.email,
        "roles" -> user.roles)
  }

  val form = Form[User](
    mapping(
      "id" -> optional(of[String] verifying pattern(
        """[a-fA-F0-9]{24}""".r,
        "constraint.objectId",
        "error.objectId")),
      "name" -> nonEmptyText,
      "password" -> tuple (
        "main" -> text(minLength=12),
        "confirm" -> text
      ).verifying("passwords don't match", p => p._1 == p._2),
      "email" -> email
      ) { (id, name, password, email) =>
      User(
        id.map(new BSONObjectID(_)), name, password._1, email, Seq())
    } { user =>
      Some(
        (user.id.map(_.stringify),
          user.name,
          ("", ""),
          user.email))
    })
}