package moviesdb
import cats.Id
import moviesdb.domain.*
import moviesdb.core.*
import moviesdb.users.UsersService
import testUtils.*

class UsersServiceSpec extends munit.FunSuite:

  private type Password = String

  private val usersList: List[(User, Password)] = List(
    (User(uid1, UserName("User1")), "$strongPwd"),
    (User(uid2, UserName("User2")), "weakPass")
  )

  private val hashFn = HashingAlgs.sha256

  private val whiteList: List[UsersRepoMockup.User] = usersList.map { (user, pwd) =>
    UsersRepoMockup.User(user.id, hashFn(pwd), user.name)
  }

  private val repo = UsersRepoMockup[Id](whiteList)

  // this is what we're testing!
  private val service: UsersService[Id] = UsersService(repo, hashFn)

  test("Should retrieve users when given correct credentials") {
    val expected: List[Option[User]] = usersList.map { (user, _) => Some(user) }
    val obtained: List[Option[User]] = usersList.map { (user, pwd) => service.getUser(user.name, pwd) }
    assertEquals(obtained, expected)
  }

  test("Should return None when given incorrect username or password") {

    val inputs: List[(UserName, Password)] =
      List((UserName("nobody"), "whatever")) ++
      usersList.map( (_, pwd) => (UserName("other"), pwd) ) ++ // wrong username
      usersList.map( (user, _) => (user.name, "wrongPwd") ) // wrong password

    val obtained = inputs.map(service.getUser.tupled).filter(_.isDefined)
    val expected = List.empty

    assertEquals(obtained, expected)
  }
