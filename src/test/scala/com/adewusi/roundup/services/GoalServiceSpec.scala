package com.adewusi.roundup.services

//import cats.effect.IO
//import com.adewusi.roundup.clients.SavingsGoalClient
//import com.adewusi.roundup.model.AppConfig
//import com.adewusi.roundup.repository.GoalRepository
//import java.util.UUID
import munit.CatsEffectSuite


class GoalServiceSpec extends CatsEffectSuite {
  test("getOrCreateGoal creates and persists goal") {
//    val inMemoryGoalRepository: GoalRepository[IO] = ???
//    val testSavingsGoalClient: SavingsGoalClient[IO] = ???
//    val service = GoalService.impl(inMemoryGoalRepository, testSavingsGoalClient)
//    val config: AppConfig = ???
//    val accountId: UUID = ???
//
//    val result = service.getOrCreateGoal(config, accountId).unsafeRunSync()
//    val expectedGoalUuid: UUID = ???
//
//    assertEquals(result, Right(expectedGoalUuid))
  }
  test("getOrCreateGoal reads and validates goal") {}
  test("getOrCreateGoal returns an error when a goal fails to be read") {}
  test("getOrCreateGoal returns an error when getGoal API does not return a success") {}
  test("getOrCreateGoal returns an error when createGoal API does not return a success") {}
  test("getOrCreateGoal returns an error if a goal cannot be persisted") {}
}
