package loadtest

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.util.Random

class WebSocketVaadinSimulation extends Simulation {
  
  val server :String = "localhost"
  val port : String = "8080"
  val cPath : String = "v-quiz"
  
//  val baseurl = """http://matti.app.fi:8081/v-quiz"""
//  val wsurl = """ws://matti.app.fi:8081/v-quiz"""
  val baseurl = s"http://$server:$port/$cPath"
  val wsurl = s"ws://$server:$port/$cPath"
  
  val httpProtocol = http
  .baseURL(baseurl)
  .inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.(t|o)tf""", """.*\.png"""), WhiteList())
  .acceptHeader("""*/*""")
  .acceptEncodingHeader("""gzip,deflate,sdch""")
  .acceptLanguageHeader("""en-US,en;q=0.8,fi;q=0.6""")
  .contentTypeHeader("""application/json; charset=UTF-8""")
  .userAgentHeader("""Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.143 Safari/537.36""")
  .wsBaseURL(wsurl)

  val headers_0 = Map(
    """Accept""" -> """text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8""",
    """Cache-Control""" -> """max-age=0""")

  val headers_1 = Map("""Cache-Control""" -> """max-age=0""")

  val headers_2 = Map(
    """Cache-Control""" -> """max-age=0""",
    """Content-type""" -> """application/x-www-form-urlencoded""",
    """Origin""" -> baseurl)

  val headers_3 = Map("""Origin""" -> baseurl)
  
  val headers_4 = Map(
    """Origin""" -> baseurl,
    """Host""" -> "matti.app.fi:8081"
  )

  val headers_5 = Map(
    """Origin""" -> baseurl,
    """Host""" -> "matti.app.fi:8081"
  )

  val uri1 = baseurl

  val chooseRandomUserAndSuggesstion = exec((session) => {
      val username: String = Random.alphanumeric.take(6).mkString
      val answer: String = Random.alphanumeric.take(7).mkString
      session.setAll(
        """answer""" -> answer,
        """username""" -> username
      )
    })
  
  // TODO separate Vaadin and Atmsphere related xsrf key, syncId, atmokey, 
  // and message lenght stuff to a helper liberary for easier test maintenance

  // static resoures not fetched, can/should be moved to CDN
  val scn = scenario("BasicVaadinHelloSimulation4")
  .group("Create Process") {
    exec(chooseRandomUserAndSuggesstion)
    .exec(http("request_0")
          .get("""/""")
          .headers(headers_0))
    .pause(1 seconds)
    // ajax communication request must be in subsequent exec,
    // otherwise (if used e.g. resource request like in recorded case)
    // session is not yet correctly set up
    .exec(http("request_2")
          .post(uri1 + """/?v-1409148708994""")
          .headers(headers_2)
          .body(RawFileBody("initial_uidl_request.txt"))
          // save xsrf preventation key for further requests
          .check(regex("""Vaadin-Security-Key\\":\\"([^\\]+)""").saveAs("seckey"))
    )
    .pause(1 seconds)
    .exec(
      ws("PUSH CHANNEL").open("/PUSH/?v-uiId=0&v-csrfToken=${seckey}&X-Atmosphere-tracking-id=0&X-Atmosphere-Framework=2.1.5.vaadin4-jquery&X-Atmosphere-Transport=websocket&X-Atmosphere-TrackMessageSize=true&X-Cache-Date=0&Content-Type=application/json;%20charset=UTF-8&X-atmo-protocol=true")
      .headers(headers_4)
      .check(wsAwait.within(30).until(1).regex("50\\|([^\\|]+).*").saveAs("atmokey"))
    )
    .pause((20 + Random.nextInt(5)) seconds)
    .exec(
      ws("LOGIN 1")
      .sendText("""331|{"csrfToken":"${seckey}", "rpc":[["1","v","v",["positionx",["i","169"]]],["1","v","v",["positiony",["i","254"]]],["0","com.vaadin.shared.ui.ui.UIServerRpc","resize",["779","845","845","779"]],["16","v","v",["c",["i","0"]]],["5","v","v",["text",["s","${username}"]]],["5","v","v",["c",["i","5"]]]], "syncId":1}""")
    )
    .pause(1 seconds)
    .exec(
      ws("LOGIN 2")
      .sendText("""303|{"csrfToken":"${seckey}", "rpc":[["7","com.vaadin.shared.ui.button.ButtonServerRpc","click",[{"shiftKey":false, "metaKey":false, "ctrlKey":false, "relativeX":"52", "relativeY":"14", "type":"1", "altKey":false, "button":"LEFT", "clientX":"258", "clientY":"464"}]]], "syncId":2}""")
      .check(wsAwait.within(5).until(1).regex("Welcome ${username}"))
    )
    .pause((20 + Random.nextInt(5)) seconds)
    .exec(
      ws("Suggest answer 1")
      .sendText("""374|{"csrfToken":"${seckey}", "rpc":[["15","v","v",["text",["s","${answer}"]]],["15","v","v",["c",["i","7"]]],["16","com.vaadin.shared.ui.button.ButtonServerRpc","click",[{"shiftKey":false, "metaKey":false, "ctrlKey":false, "relativeX":"82", "relativeY":"8", "type":"1", "altKey":false, "button":"LEFT", "clientX":"316", "clientY":"327"}]]], "syncId":3}""")
      .check(wsAwait.within(15).until(1).regex("wrong"))
    )
    .pause((20 + Random.nextInt(5)) seconds)
    .exec(chooseRandomUserAndSuggesstion)
    .exec(
      ws("Suggest answer 2")
      .sendText("""374|{"csrfToken":"${seckey}", "rpc":[["15","v","v",["text",["s","${answer}"]]],["15","v","v",["c",["i","7"]]],["16","com.vaadin.shared.ui.button.ButtonServerRpc","click",[{"shiftKey":false, "metaKey":false, "ctrlKey":false, "relativeX":"82", "relativeY":"8", "type":"1", "altKey":false, "button":"LEFT", "clientX":"316", "clientY":"327"}]]], "syncId":4}""")
      .check(wsAwait.within(15).until(1).regex("wrong"))
    )
    .pause((20 + Random.nextInt(5)) seconds)
    .exec(chooseRandomUserAndSuggesstion)
    .exec(
      ws("Suggest answer 3")
      .sendText("""374|{"csrfToken":"${seckey}", "rpc":[["15","v","v",["text",["s","${answer}"]]],["15","v","v",["c",["i","7"]]],["16","com.vaadin.shared.ui.button.ButtonServerRpc","click",[{"shiftKey":false, "metaKey":false, "ctrlKey":false, "relativeX":"82", "relativeY":"8", "type":"1", "altKey":false, "button":"LEFT", "clientX":"316", "clientY":"327"}]]], "syncId":5}""")
      .check(wsAwait.within(15).until(1).regex("wrong"))
    )
    .pause(3 seconds)
    .exec(ws("WS close").close)
    .exec(
      http("atmosphere close").get("/PUSH/?v-uiId=0&v-csrfToken=${seckey}&X-Atmosphere-Transport=close&X-Atmosphere-tracking-id=${atmokey}&foo=bar")
      .headers(Map(
          """Origin""" -> baseurl,
          """Host""" -> "matti.app.fi:8081",
          "X-Atmosphere-tracking-id" -> "${atmokey}"
          
        ))
    )
    .pause(2 seconds)
  };

  // Next line will execute the test just once
  //setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

  // This uses more load, simulates 1000 users who arrive with-in 10 seconds
  setUp(scn.inject(rampUsers(1) over (30 seconds))).protocols(httpProtocol)
  
}