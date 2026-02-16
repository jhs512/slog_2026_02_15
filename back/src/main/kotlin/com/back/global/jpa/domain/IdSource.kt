package com.back.global.jpa.domain

import com.back.global.app.app.AppFacade

interface IdSource {
    val entityName: String

    fun newId(): Long = AppFacade.idGenerator.genId(entityName)
}
