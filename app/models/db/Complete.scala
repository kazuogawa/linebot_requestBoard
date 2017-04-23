package models.db
import org.joda.time.DateTime
import scalikejdbc._

case class Complete (
  id: Int,
  user_id: Int,
  order_id: Int,
  created: DateTime
)

object Complete extends SQLSyntaxSupport[Complete] {

  override val tableName = "completes"
  override val columns = Seq("id", "user_id", "order_id", "created")

  def apply(c: SyntaxProvider[Complete])(rs: WrappedResultSet):Complete = apply(c.resultName)(rs)
  def apply(c: ResultName[Complete])(rs: WrappedResultSet): Complete = new Complete(
    id = rs.get(c.id),
    user_id = rs.get(c.user_id),
    order_id = rs.get(c.order_id),
    created = rs.get(c.created)
  )

  val c = Complete.syntax("c")

  def create(user_id: Int, order_id: Int, created: DateTime = new DateTime())(implicit session: DBSession = autoSession): Complete = {
    val id = withSQL{
      insert.into(Complete).namedValues(
        column.user_id -> user_id,
        column.order_id -> order_id,
        column.created -> created.toString("yyyy/MM/dd HH:mm:ss")
      )
    }.updateAndReturnGeneratedKey.apply().toInt

    Complete(
      id = id,
      user_id = user_id,
      order_id = order_id,
      created = created
    )
  }
}
