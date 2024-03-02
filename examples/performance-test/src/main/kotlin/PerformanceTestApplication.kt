package io.holixon.axon.projection.adhoc.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PerformanceTestApplication

fun main(args: Array<String>) {
  runApplication<PerformanceTestApplication>(*args)
}
