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
  override val columns = Seq("id", "user_id", "order_id", "creaeted")

  def apply(c: SyntaxProvider[Complete])(rs: WrappedResultSet):Complete = apply(c.resultName)(rs)
  def apply(c: ResultName[Complete])(rs: WrappedResultSet): Complete = new Complete(
    id = rs.get(c.id),
    user_id = rs.get(c.user_id),
    order_id = rs.get(c.order_id),
    created = rs.get(c.created)
  )

  val u = Complete.syntax("c")

  def create(user_id: Int, order_id: Int, created: DateTime = DateTime.now)(implicit session: DBSession = AutoSession): Complete = {
    val id = withSQL{
      insert.into(Complete).namedValues(
        column.user_id -> user_id,
        column.order_id -> order_id,
        column.created -> created
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
