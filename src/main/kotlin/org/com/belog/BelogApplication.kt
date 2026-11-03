package org.com.belog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BelogApplication

fun main(args: Array<String>) {
    runApplication<BelogApplication>(*args)
}
