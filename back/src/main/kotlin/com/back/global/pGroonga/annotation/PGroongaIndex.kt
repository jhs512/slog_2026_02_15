package com.back.global.pGroonga.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class PGroongaIndex(
    val columns: Array<String>,
    val tokenizer: String = "TokenBigram",
)
