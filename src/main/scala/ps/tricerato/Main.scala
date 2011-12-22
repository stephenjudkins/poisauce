import collection.immutable.SortedMap
import collection.JavaConversions
import java.io.ByteArrayInputStream
import org.apache.poi.ss.usermodel.{Sheet, WorkbookFactory}
import unfiltered.netty.ReceivedMessage
import unfiltered.request._
import unfiltered.response._
import JavaConversions._
import util.Properties

object Main {
  val plan = unfiltered.netty.cycle.Planify {
     case req @ POST(Path("/")) => response(req)
     case GET(Path("/")) => Ok ~> ResponseString("instructions go here")
  }

  def response(request: HttpRequest[ReceivedMessage]) = {
    try {
      Ok ~> JsonContent ~> ResponseString(parse(Body.bytes(request)))
    } catch {
      case t: Throwable => {
        InternalServerError ~> ResponseString(t.getMessage)
      }
    }
  }


  def main(args:Array[String]) {
    val port = Properties.envOrElse("PORT", "8080").toInt
    unfiltered.netty.Http(port).chunked(10000000).plan(plan).run()
  }

  import net.liftweb.json._
  import JsonDSL._

  def parse(doc: Array[Byte]) = {

    val is = new ByteArrayInputStream(doc)
    val workbook = WorkbookFactory.create(is)
    val sheets = (0 until workbook.getNumberOfSheets).map(workbook.getSheetAt(_))

    val json = ("sheets" -> sheets.map(buildSheet))

    pretty(render(json))
  }

  def buildSheet(sheet: Sheet) = {
    val rows = sheet.toSeq.map { row =>
      val rowMap = row.toSeq.foldLeft(SortedMap.empty[Int, JValue]) { case (m, c) =>
        import org.apache.poi.ss.usermodel.Cell._
        val value = c.getCellType match {
          case CELL_TYPE_BLANK => JNull
          case CELL_TYPE_NUMERIC => JDouble(c.getNumericCellValue)
          case CELL_TYPE_STRING => JString(c.getStringCellValue)
          case CELL_TYPE_FORMULA => JString(c.getCellFormula)
          case CELL_TYPE_BOOLEAN => JBool(c.getBooleanCellValue)
          case CELL_TYPE_ERROR => JNull
        }
        m + (c.getColumnIndex -> value)
      }

      (0 until rowMap.lastOption.map(_._1 + 1).getOrElse(0)) map { rowMap.get(_).getOrElse(JNull) }
    } filterNot { _.isEmpty }

    ("name" -> sheet.getSheetName) ~ ("rows" -> rows)
  }
}