package com.back.global.jpa.domain

import com.back.global.app.app.AppFacade

interface IdSource {
    val entityName: String

    fun newId(count: Int = 1): Long = AppFacade.idGenerator.genId(entityName, count)
}
