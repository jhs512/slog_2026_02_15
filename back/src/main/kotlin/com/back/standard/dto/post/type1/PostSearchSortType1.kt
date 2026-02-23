package com.back.standard.dto.post.type1

import org.springframework.data.domain.Sort

enum class PostSearchSortType1 {
    CREATED_AT,
    CREATED_AT_ASC,
    MODIFIED_AT,
    MODIFIED_AT_ASC;

    val sortBy: Sort by lazy {
        Sort.by(
            if (isAsc) Sort.Direction.ASC else Sort.Direction.DESC,
            property
        )
    }

    val property by lazy {
        when (this) {
            CREATED_AT, CREATED_AT_ASC -> "createdAt"
            MODIFIED_AT, MODIFIED_AT_ASC -> "modifiedAt"
        }
    }

    val isAsc by lazy {
        name.endsWith("_ASC")
    }
}
