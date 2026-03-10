package co.kofixtures.examples

data class User(val name: String, val age: Int)

data class Address(val street: String, val city: String)

data class Profile(
    val user: User,
    val address: Address,
    val status: Status,
)

enum class Status { ACTIVE, INACTIVE, SUSPENDED }
