package com.back.global.jpa.domain

interface IdSource {
    val entityName: String

    fun newId(): Long = 0L
}
