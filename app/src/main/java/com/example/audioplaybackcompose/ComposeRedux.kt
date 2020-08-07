@file:Suppress("FunctionName")

package com.example.audioplaybackcompose

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state

typealias Dispatch<A> = (action: A) -> Unit
typealias Reducer<S, A> = (state: S, action: A) -> S
typealias Middleware<S, A> = (state: S, action: A, next: (A) -> S) -> S

fun <S, A> preReducerMiddleware(block: (state: S, action: A) -> Unit): Middleware<S, A> {
  return { state: S, action: A, next: (A) -> S ->
    block(state, action)
    next(action)
  }
}

fun <S, A> postReducerMiddleware(block: (previousState: S, latestState: S, action: A) -> Unit): Middleware<S, A> {
  return { state: S, action: A, next: (A) -> S ->
    val newState = next(action)
    block(state, newState, action)
    newState
  }
}

@Composable
fun <S, A> redux(
  initialState: S,
  reducer: Reducer<S, A> = { state, _ -> state },
  middlewares: List<Middleware<S, A>> = listOf { _, action, next -> next(action) }
): Pair<S, Dispatch<A>> {
  val (state, setState) = state { initialState }

  val reducedMiddleware = middlewares.reduce { acc, middleware ->
    { state, action, next ->
      middleware(state, action) { nextAction ->
        acc(state, nextAction) { accAction ->
          next(accAction)
        }
      }
    }
  }

  return state to { action ->
    reducedMiddleware(state, action) { middlewareAction ->
      reducer(state, middlewareAction).also(setState)
    }
  }
}

abstract class ReduxActivity<S, A> : AppCompatActivity() {
  fun PreReducerMiddleware(block: (state: S, action: A) -> Unit): Middleware<S, A> =
    preReducerMiddleware(block)

  fun PostReducerMiddleware(block: (previousState: S, latestState: S, action: A) -> Unit): Middleware<S, A> =
    postReducerMiddleware(block)

  fun Middleware(middleware: Middleware<S, A>): Middleware<S, A> = middleware
  fun Reducer(reducer: Reducer<S, A>) = reducer

  @Composable
  fun Redux(
    initialState: S,
    reducer: Reducer<S, A> = { state, _ -> state },
    middlewares: List<Middleware<S, A>> = listOf { _, action, next -> next(action) }
  ): Pair<S, Dispatch<A>> = redux(initialState, reducer, middlewares)
}

interface StateAction<S, A> {
  fun PreReducerMiddleware(block: (state: S, action: A) -> Unit): Middleware<S, A> =
    preReducerMiddleware(block)

  fun PostReducerMiddleware(block: (previousState: S, latestState: S, action: A) -> Unit): Middleware<S, A> =
    postReducerMiddleware(block)

  fun Middleware(middleware: Middleware<S, A>): Middleware<S, A> = middleware
  fun Reducer(reducer: Reducer<S, A>) = reducer

  @Composable
  fun Redux(
    initialState: S,
    reducer: Reducer<S, A> = { state, _ -> state },
    middlewares: List<Middleware<S, A>> = listOf { _, action, next -> next(action) }
  ): Pair<S, Dispatch<A>> = redux(initialState, reducer, middlewares)
}
