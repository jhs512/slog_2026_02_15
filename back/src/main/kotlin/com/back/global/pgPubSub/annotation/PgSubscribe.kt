package com.back.global.pgPubSub.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PgSubscribe(val channel: String)
