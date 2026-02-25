package com.back.global.pgpubsub.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PgSubscribe(val channel: String)
