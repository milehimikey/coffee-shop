package wtf.milehimikey.coffeeshop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CoffeeShopApplication

fun main(args: Array<String>) {
    runApplication<CoffeeShopApplication>(*args)
}
