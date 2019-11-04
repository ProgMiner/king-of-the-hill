package ru.byprogminer.kingofthehill

data class Player(
    val user: User,

    val field: Field,
    val power: Int = 1,
    val items: List<Item> = listOf()
    // TODO
)
