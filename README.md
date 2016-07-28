## hAkker [![Build Status][travis-image]][travis-url] [![license][license-image]][license-url] [![Coverage Status][coveralls-image]][coveralls-url]
[travis-image]: http://img.shields.io/travis/ulasakdeniz/hakker/master.svg
[travis-url]: https://travis-ci.org/ulasakdeniz/hakker
[coveralls-image]: https://coveralls.io/repos/github/ulasakdeniz/hakker/badge.svg?branch=master
[coveralls-url]: https://coveralls.io/github/ulasakdeniz/hakker?branch=master
[license-image]: https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=2592000
[license-url]: https://github.com/ulasakdeniz/hakker/blob/master/LICENSE

**Work in Progress**

A backend web framework being developed with Akka.

### Usage
Add the following to your `build.sbt`:

```libraryDependencies += "com.ulasakdeniz.hakker" %% "hakker-core" % "0.1.1"```

You need at least one `Controller` to define paths and behaviors of your application's routes. You should use [akka-http's Routing DSL](http://doc.akka.io/docs/akka/2.4.8/scala/http/routing-dsl/index.html) for routes. Here is an example:

```scala
package com.example

import com.ulasakdeniz.hakker.Controller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

object Application extends Controller {
  override def route: Route = {
    get {
      pathSingleSlash {
        complete("hello world")
      }
    }
  }
}
```

Rendering an `HTML` file instead of plain text requires to specify frontend directory path (`hakker.frontend.frontend-path`) in the [configuration file](https://github.com/ulasakdeniz/hakker-sample/blob/master/src/main/resources/application.conf). Then use `render` function:

```scala
path("index") {
  // to render index.html in the frontend/html directory
  render("index")
}
```

You also need a `LifeCycle` and put all your `Controller`s in its `boot` method:

```scala
package com.example

import com.ulasakdeniz.hakker.{Controller, LifeCycle}

object Boot extends LifeCycle {

  override def boot: List[Controller] = List(Application)

  override def beforeStart: Unit = {
    println("starting")
  }

  override def afterStop: Unit = {
    println("stopping")
  }
}
```

`hakker` provides easy to use WebSocket `Flow`s and OAuth support. For a more detailed example you should see [sample app](https://github.com/ulasakdeniz/hakker-sample).
