package xyz.malefic.test

import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.textLine

fun main() {
    if (System.console() != null) {
        var userInput = ""
        session {
            section {
                textLine("Enter your name:")
                input()
            }.runUntilInputEntered {
                onInputEntered {
                    userInput = input
                }
            }
        }
        println("You entered: $userInput")
    } else {
        println("Enter your name:")
        val result = readln()
        println("You entered: $result")
    }
}
