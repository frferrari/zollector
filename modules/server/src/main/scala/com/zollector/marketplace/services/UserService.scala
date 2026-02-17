package com.zollector.marketplace.services

import zio.*
import com.zollector.marketplace.repositories.UserRepository
import com.zollector.marketplace.domain.data.User
import com.zollector.marketplace.http.requests.RegisterUserRequest

import java.security.SecureRandom
import java.time.Instant
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {
  def registerUser(req: RegisterUserRequest): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
}

class UserServiceLive private (userRepo: UserRepository) extends UserService {
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
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Can't verify user $email"))
      result <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
    } yield result
}

object UserServiceLive {
  val layer = ZLayer {
    ZIO.service[UserRepository].map(repo => new UserServiceLive(repo))
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
    println(UserServiceLive.Hasher.generateHash("admin@zollector.com"))
    println(
      UserServiceLive.Hasher.validateHash(
        "admin@zollector.com",
        "1000:4A6F579C455FBA4C0ED77A1517EF9546D5E1A3B8EF9E3033:C4947E73B50A06A5CF3B2EF3036718BFB8E763BDEECF0B1B"
      )
    )
  }
}
