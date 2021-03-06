---
id: reverse-routing
title: Reverse Routing
---

Each `Route` also contains enough information to generate its own URL given the necessary parameters. This lets you
build type-safe and reliable references to your application's URLs without any hardcoding. Assuming you have a route:

```scala mdoc
import routing._

val BlogPost = Method.GET / "post" / pathVar[String]("slug") :? queryParam[Int]("id")
```

Then you can apply the necessary parameters in a number of ways to get various parts of the URL:

```scala mdoc
BlogPost.path("test-slug", 1)
BlogPost.query("test-slug", 1)
BlogPost.uri("test-slug", 1)
// Alias of `uri`
BlogPost.url("test-slug", 1)
```

To avoid having to pass the parameters to each of these methods, you can eagerly apply the parameters to convert a
`Route` to a `Call`:

```scala mdoc
val blogPost = BlogPost("test-slug", 1)

blogPost.path
blogPost.query
blogPost.uri
blogPost.url
```
