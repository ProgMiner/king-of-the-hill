package ru.byprogminer.kingofthehill

interface Field {

    val dropItems: Boolean

    val peak: Boolean
    val nearThePeak: Field?
}
