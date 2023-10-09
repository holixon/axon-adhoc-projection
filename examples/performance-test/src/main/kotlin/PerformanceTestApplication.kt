package io.holixon.selectivereplay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PerformanceTestApplication

fun main(args: Array<String>) {
  runApplication<PerformanceTestApplication>(*args)
}
