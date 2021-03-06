package org.apache.spark.orientdb.udts

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ORecord
import org.apache.spark.sql.catalyst.util.{ArrayData, GenericArrayData}
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

@SQLUserDefinedType(udt = classOf[LinkType])
case class Link(element: OIdentifiable) extends Serializable {
  override def hashCode(): Int = {
    var hashCode = 1

    val elemValue = if (element == null) 0 else element.hashCode()
    hashCode = 31 * hashCode + elemValue
    hashCode
  }

  override def equals(other: scala.Any): Boolean = other match {
    case that: Link => that.element.equals(this.element)
    case _ => false
  }

  override def toString: String = element.toString
}

class LinkType extends UserDefinedType[Link] {

  override def sqlType: DataType = ArrayType(StringType)

  override def serialize(obj: Link): Any = {
    val out = new ByteArrayOutputStream()
    val os = new ObjectOutputStream(out)
    if (obj.element.isInstanceOf[ORecord])
      os.writeObject(obj.element.asInstanceOf[ORecord])
    else
      os.writeObject(obj.element.asInstanceOf[ORecordId])
    new GenericArrayData(Array(UTF8String.fromBytes(out.toByteArray)))
  }

  override def deserialize(datum: Any): Link = {
    datum match {
      case values: ArrayData =>
        new Link(values.toArray[UTF8String](StringType).map { elem =>
          val in = new ByteArrayInputStream(elem.getBytes)
          val is = new ObjectInputStream(in)
          val data = is.readObject()
          if (data.isInstanceOf[ORecord]) {
            data.asInstanceOf[ORecord]
          } else {
            data.asInstanceOf[ORecordId]
          }
        }.head)
      case other => sys.error(s"Cannot deserialize $other")
    }
  }

  override def userClass: Class[Link] = classOf[Link]
}

object LinkType extends LinkType

