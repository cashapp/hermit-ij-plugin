package com.squareup.cash.hermit

import arrow.core.Either

typealias Error = String
typealias Result<T> = Either<Error, T>
typealias Failure = Either.Left<Error>
typealias Success<T> = Either.Right<T>

fun <T> success(v: T): Result<T> {
    return Either.Right(v)
}

fun <T> failure(error: Error): Result<T> {
    return Either.Left(error)
}
