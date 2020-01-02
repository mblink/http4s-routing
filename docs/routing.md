---
id: routing
title: Routing
---

Once you've built your `Route`s, they can be used to route http4s requests to specific application logic. Each route contains enough information to match it against a request and extract the relevant parameters from the request's path and query string, so all you need to do is specify the route-specific handling logic. Assuming you have the test routes:

```scala mdoc
import cats.instances.int._
import cats.instances.string._
import org.http4s.dsl.io._
import org.http4s.routing._

val Login = GET / "login"
val Hello = GET / "hello" / ("name" -> StringVar)
val BlogPost = GET / "post" / ("slug" -> StringVar) :? queryParam[Int]("id")
```

Then you can specify the handling logic for a given route by calling the `handled` method. `handled` takes a function of the type:

```scala
// where `P` is a tuple of the route's parameter types
Request[F] => P => F[Response[F]]
```

from the route's parameters to an http4s response (`F[Response[F]]`).

```scala mdoc
import cats.effect.IO
import org.http4s.Request

val handledLogin = Login.handle.with_.logic[IO](_ => _ => Ok("Login page"))
val handledHello = Hello.handle.with_.logic[IO](_ => name => Ok(s"Hello, $name"))
val handledBlogPost = BlogPost.handle.with_.logic((req: Request[IO]) => { case (slug, id) =>
  Ok(s"Blog post with id: $id, slug: $slug found at ${req.uri}")
})
```

And finally, you can compose your handled routes into a service by passing them to `Route.httpRoutes`:

```scala mdoc
import cats.effect.IO
import org.http4s.HttpRoutes

val service: HttpRoutes[IO] = Route.httpRoutes(
  handledLogin,
  handledHello,
  handledBlogPost
)
```

You can confirm that routes are matched correctly by passing some test requests to the service:

```scala mdoc
import org.http4s.Request

def testRoute(call: Call) =
  service
    .run(Request[IO](method = call.route.method, uri = call.uri))
    .value
    .unsafeRunSync
    .get
    .as[String]
    .unsafeRunSync

testRoute(Login())
testRoute(Hello("world"))
testRoute(BlogPost("my-slug", 1))
```

You can also check that requests matching none of your routes are not handled by the service:

```scala mdoc
import org.http4s.{Method, Uri}

def unhandled(method: Method, path: String) =
  service
    .run(Request[IO](method = method, uri = Uri(path = path)))
    .value
    .unsafeRunSync

unhandled(GET, "/fake")

// Not handled by `Hello` because the method doesn't match
unhandled(POST, "/hello/world")
```