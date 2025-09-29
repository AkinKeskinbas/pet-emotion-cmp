package com.keak.petemotions.misc

fun Boolean?.orFalse(): Boolean {
    return this ?: false
}

fun Int?.orZero():Int{
    return this ?: 0
}