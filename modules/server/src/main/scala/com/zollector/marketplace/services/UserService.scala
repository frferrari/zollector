package com.zollector.marketplace.services

import zio.*

import java.security.SecureRandom
import java.time.Instant
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import com.zollector.marketplace.repositories.UserRepository
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.http.requests.*

trait UserService {
  def registerUser(req: RegisterUserRequest): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(req: UpdatePasswordRequest): Task[User]
  def deleteUser(req: DeleteUserRequest): Task[Boolean]
  def generateToken(req: LoginRequest): Task[Option[UserToken]]
}

class UserServiceLive private (jwtService: JWTService, userRepo: UserRepository)
    extends UserService {
  override def registerUser(req: RegisterUserRequest): Task[User] =
    userRepo.create(
      User(
        id = -1L,
        nickname = req.nickname,
        email = req.email,
        hashedPassword = UserServiceLive.Hasher.generateHash(req.password),
        firstName = req.firstName,
        lastName = req.lastName,
        createdAt = Instant.now()
      )
    )

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- userRepo.getByEmail(email)
      result <- existingUser match {
        case Some(user) =>
          ZIO
            .attempt(UserServiceLive.Hasher.validateHash(password, user.hashedPassword))
            .orElseSucceed(false)
        case None =>
          ZIO.succeed(false)
      }
    } yield result

  override def updatePassword(req: UpdatePasswordRequest): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(req.email)
        .someOrFail(new RuntimeException(s"Can't verify user ${req.email}, not found"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(req.oldPassword, existingUser.hashedPassword)
      )
      updatedUser <- userRepo
        .update(
          existingUser.id,
          existingUser.copy(hashedPassword = UserServiceLive.Hasher.generateHash(req.newPassword))
        )
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not update password for user ${req.email}"))
    } yield updatedUser

  override def deleteUser(req: DeleteUserRequest): Task[Boolean] =
    for {
      existingUser <- userRepo
        .getByEmail(req.email)
        .someOrFail(new RuntimeException(s"Can't verify user ${req.email}, not found"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(req.password, existingUser.hashedPassword)
      )
      isUserDeleted <- userRepo
        .delete(existingUser.id)
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not delete user ${req.email}"))
    } yield isUserDeleted

  override def generateToken(req: LoginRequest): Task[Option[UserToken]] =
    for {
      existingUser <- userRepo
        .getByEmail(req.email)
        .someOrFail(new RuntimeException(s"Can't verify user ${req.email}"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(req.password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken
}

object UserServiceLive {
  val layer = ZLayer {
    for {
      jwtService <- ZIO.service[JWTService]
      userRepo   <- ZIO.service[UserRepository]
    } yield new UserServiceLive(jwtService, userRepo)
  }

  object Hasher {
    private val PBKDF2_ALGO: String    = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int = 1000
    private val SALT_BYTE_SIZE: Int    = 24
    private val HASH_BYTE_SIZE: Int    = 24
    private val skf: SecretKeyFactory  = SecretKeyFactory.getInstance(PBKDF2_ALGO)

    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] = {

      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)

      skf.generateSecret(keySpec).getEncoded
    }

    private def toHex(array: Array[Byte]): String =
      array.map(byte => "%02X".format(byte)).mkString

    private def fromHex(hexString: String): Array[Byte] =
      hexString.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }

    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }

      diff == 0
    }

    def generateHash(password: String): String = {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt) // A 24 random bytes
      val hashBytes = pbkdf2(password.toCharArray, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }

    def validateHash(password: String, hashedPassword: String): Boolean = {
      val hashSegments = hashedPassword.split(":")
      val nIterations  = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(password.toCharArray, salt, nIterations, HASH_BYTE_SIZE)

      compareBytes(testHash, validHash)
    }

  }
}

object UserServiceDemo {
  def main(args: Array[String]) = {
    val password = "bobPassword"
    println(UserServiceLive.Hasher.generateHash(password))
    println(
      UserServiceLive.Hasher.validateHash(
        password,
        "1000:957CFE57B3A3C7FE1888AA9E00FB2E05E385EE6330A89374:7D59DE2824017BE7ED869B50D41F1A8F3A32AF05E82D2E00"
      )
    )
  }
}
