package io.holixon.selectivereplay.rest

import io.holixon.selectivereplay.model.CurrentBalanceModel
import io.holixon.selectivereplay.model.CurrentBalanceModelRepository
import jakarta.websocket.server.PathParam
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/bankaccount")
class BankController(
  val currentBalanceImmutableModelRepository: CurrentBalanceModelRepository
) {
  @GetMapping("/{bankAccountId}")
  fun getBalance(@PathVariable("bankAccountId") bankAccountId: String): ResponseEntity<CurrentBalanceModel> {
    currentBalanceImmutableModelRepository.findById(bankAccountId).orElse(null)?.let {
      return ResponseEntity.ok(it)
    }

    return ResponseEntity.notFound().build()
  }
}
