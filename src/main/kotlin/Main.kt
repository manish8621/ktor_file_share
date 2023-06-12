import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import java.io.File
import kotlin.io.use

//constants
const val HOST = "localhost"//"192.168.0.108"
const val PORT = 8080
const val ROOT_DIR = "/"
const val STATIC_ROOT_DIR = "res"
const val UPLOAD_PATH = "C:\\Users\\MK\\Documents\\workspace\\"


//temp vars
var sharedText = "Have a Good Day Master"

var serverName = "MKServer"
val authorizedClients = hashSetOf<String>()
var sharedTextHistory = arrayListOf(sharedText)


fun main(args: Array<String>) {
    embeddedServer(
        Netty,
        host = HOST,
        port = PORT,
        module = Application::module
    ).start()
}

fun Application.module() {
//    intercept(ApplicationCallPipeline.Call){
//        if(call.request.path().startsWith("/auth")){
//            proceed()
//        }
//        //check if authorized
//        val clientIp = call.request.origin.remoteHost
//        if (!authorizedClients.contains(clientIp)) {
//                call.respondText(status = HttpStatusCode.Forbidden, text = "Access Forbidden")
//                return@intercept finish()
//        }
//
//    }
    apiModule()
    htmlModule()
    staticContentModule()
}

fun Application.apiModule() {
    routing {

        //handles text share
        sharedTextHandler()
        //end point to get list of files for a given path
        filesListApi()
        //to download and upload files
        filesHandler()
        //handles other info like server name and etc
        infoHandler()
        //handles authentication
        authHandler()
    }
}

fun Application.htmlModule() {
    routing {
        get("/") {
            //respond html file
            call.respondFile(File(STATIC_ROOT_DIR, "index.html"), configure = {
                //set content type
                setProperty(AttributeKey("ContentType"),ContentType.Text.Html)

            })
        }
    }
}
fun Application.staticContentModule() {
    routing {
        static("/static") {
            staticRootFolder = File(".").also { println(it.absolutePath) }
            files("res")
        }
    }
}

fun Routing.infoHandler() {
    route("/info") {
        get {
            val response = "{name : \"$serverName\"} ,"
            call.respondText(response)
        }
    }
}
fun Routing.authHandler() {
    route("/auth") {
        get {
            //get input from kotlin console todo:make it as lambda
            print("Do you want to authorize this client? (y/n) : ")
            val input = readLine()
            if (input.equals("n",true)){
                call.respondText(status = HttpStatusCode.Forbidden, text = "Access Forbidden")
                return@get
            }
            val clientIp = call.request.origin.remoteHost
            authorizeClient(clientIp)
            call.respondText(status = HttpStatusCode.Accepted, text = "OK")
        }
        post {
            call.receive<String>()
        }
    }
}
fun Routing.filesHandler() {
    route("/file") {
        get {
            val path = call.request.queryParameters["path"]

            path?.let{
                val file = File(it)
                if (file.exists() && file.isFile) {
                    call.response.header("Content-Disposition", "attachment; filename=${file.name}")
                    call.respondFile(
                        file.parentFile, fileName = file.name,
                    )
                }
            }
            call.respondText(status = HttpStatusCode(404, "file not found"), text = "not found")
        }
        post {
            call.receiveMultipart().forEachPart { part->
                if(part is PartData.FileItem){
                    val file = File(UPLOAD_PATH + part.originalFileName)
                    part copyTo file
                }
            }
            call.respondText(text="ok", status = HttpStatusCode(200, "OK"))
        }
    }
}




fun Routing.sharedTextHandler() {
    route("/text") {
        get {
            this.call.respondText(sharedText)
        }
        route("/history"){
            get {
                this.call.respondText(sharedTextHistory.toString())
            }
        }
        post {
            sharedText = call.receiveText()
            this.call.respond("ok")
            sharedTextHistory.add(sharedText)
        }
    }
}


fun Routing.filesListApi() {
    route("/list_files") {
        get {
            var statusCode = HttpStatusCode(404, "folder not found")
            var response = ""
            val path = call.request.queryParameters["path"] ?: ROOT_DIR
            val folder = File(path)
            //check folder exists and get list of files and folders
            if (folder.exists()) {
                statusCode = HttpStatusCode(200, "OK")
                response = getFilesListJson(folder)
            }
            call.respondText(text = response, status = statusCode)
        }
    }
}

fun getFilesListJson(folder: File): String {
    return folder.listFiles().let { items ->
        val json = ""
        //splits into pair based on predicate
        val filteredList = items?.partition { it.isDirectory }
        val folders = filteredList?.first?.map { it.name }
        val files = filteredList?.second?.map { it.name }
        "{ folders : $folders, files : $files}}"
    }
}
fun authorizeClient(clientIp:String){
    authorizedClients.add(clientIp)
}

infix fun PartData.FileItem.copyTo(file:File){
    streamProvider().use { inputStream->
        file.outputStream().use {
            inputStream.copyTo(it)
        }
    }
}



//todo: add html page support (static pages)
//todo: add name image for a server
//todo: add authentication for server
//todo: implement authentication for all routes of server
//todo: add a data class for client info (ip, key) the key will authenticate
//idea: add a default name for app on android that will be easy to identify