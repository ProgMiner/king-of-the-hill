package ru.byprogminer.kingofthehill

/**
 * Field of game board
 */
interface Field {

    val dropItems: Boolean
    val droppedItems: List<Item>

    val peak: Boolean
    val nearThePeak: Field?
}
