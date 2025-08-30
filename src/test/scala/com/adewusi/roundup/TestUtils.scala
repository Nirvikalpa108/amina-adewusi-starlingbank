package com.adewusi.roundup

import com.adewusi.roundup.model.{AppConfig, StarlingConfig}
import org.http4s.Uri

trait TestUtils {
  val testToken       = "test-token"
  val testBaseUri = Uri.unsafeFromString("https://api-sandbox.starlingbank.com")
  val testConfig = AppConfig(
    StarlingConfig(
      accessToken = testToken,
      baseUri = testBaseUri,
      initialGoalId = None,
      startDate = None
    )
  )
}
