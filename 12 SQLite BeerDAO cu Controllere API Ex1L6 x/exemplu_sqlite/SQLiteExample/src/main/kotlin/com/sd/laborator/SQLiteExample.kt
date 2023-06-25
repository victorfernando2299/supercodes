package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SQLiteExample

fun main(args: Array<String>) {
    runApplication<SQLiteExample>(*args)
}