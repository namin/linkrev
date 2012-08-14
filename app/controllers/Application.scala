package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent.Akka
import play.api.libs.json._

object Application extends Controller {
  val neo4jUrl = Option.apply(System.getenv("NEO4J_URL")).getOrElse("http://localhost:7474")
  val neo4jLogin = System.getenv("NEO4J_LOGIN")
  val neo4jPassword = System.getenv("NEO4J_PASSWORD")

  def gremlin(script: String, params: JsObject = JsObject(Seq())) = {
    WS.url(neo4jUrl + "/db/data/ext/GremlinPlugin/graphdb/execute_script").
    withAuth(neo4jLogin, neo4jPassword, com.ning.http.client.Realm.AuthScheme.BASIC).
    post(JsObject(Seq(
        "script" -> JsString(script),
        "params" -> params
    ))) map { _.json }
  }

  def gremlinExec(script: String) = {
    gremlin(script).value.get
  }

  def gremlinAsync(msg: String, script: String) {
    gremlin(script).onRedeem(_ => ())
    println(msg)
  }

  def create_graph(): Unit = {
    println("Starting...")

    if (gremlinExec("g.idx('vertices')[[type:'URL']].count()").asOpt[Int].getOrElse(0) > 0) {
      println("Graph already exists.")
      return
    }

    if (gremlinExec("g.indices").as[Array[String]].isEmpty) {
      gremlin("g.createAutomaticIndex('vertices', Vertex.class, null);")
      if (gremlinExec("g.V.count()").asOpt[Int].getOrElse(0) > 0)
        gremlinExec("AutomaticIndexHelper.reIndexElements(g, g.idx('vertices'), g.V);")
    }

    gremlinAsync("Creating the graph is going to take some time, watch it on " + neo4jUrl, """
            g.setMaxBufferSize(1000);

            'http://lampwww.epfl.ch/~amin/dat/mit.txt'.toURL().eachLine { def line ->
              def urls = line.split(' ');
              def url = urls[0];
              def urlHits = g.idx(Tokens.T.v)[[url:url]].iterator();
              def urlVertex = urlHits.hasNext() ? urlHits.next() : g.addVertex(['type':'URL', 'url':url]);
              urls.tail().each { def fromUrl ->
                def hits = g.idx(Tokens.T.v)[[genera:fromUrl]].iterator();
                def fromUrlVertex = hits.hasNext() ? hits.next() : g.addVertex(['type':'URL', 'url':fromUrl]);
                g.addEdge(fromUrlVertex, urlVertex, 'linksTo');
              }
            };
            g.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);""")
  }

  val urlForm = Form("url" -> nonEmptyText)
  
  def index = Action {
    Ok(views.html.index(urlForm))
  }

  def linkrev = Action { implicit request =>
    urlForm.bindFromRequest.fold(
      urlForm => Ok(views.html.index(urlForm)),
      url => Async { handleTimeout {
        linksTo(url) map { links =>
          Ok(views.html.linkrev(url, urlForm.bindFromRequest, links,
             if (links.isEmpty) "URL not in reverse index." else "URL in index."))
      }}})
  }

  def linksTo(url: String) = {
    gremlin("g.idx(Tokens.T.v)[[url:url]].inE('linksTo').outV.url",
            JsObject(Seq("url" -> JsString(url))))
    .map(_.as[List[String]])
  }

  private def handleTimeout(promise: Promise[Result]) = {
    promise orTimeout("Timed out while waiting for response", 120, java.util.concurrent.TimeUnit.SECONDS) map { _.fold (
      page => page,
      errorMsg => InternalServerError(views.html.error(errorMsg))  
    )}
  }

}
