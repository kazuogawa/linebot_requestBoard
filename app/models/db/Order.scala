package models.db

import org.joda.time.DateTime
import scalikejdbc._


case class Order(
  id:Int,
  user_id: Int,
  user : Option[User] = None,
  contents: String,
  endflag: Boolean,
  notificationflag: Boolean,
  created: DateTime
)

object Order extends SQLSyntaxSupport[Order]{
  override val tableName = "orders"

  override val columns = Seq("id", "user_id", "contents", "endflag", "notificationflag", "created")

  def apply(o: SyntaxProvider[Order])(rs: WrappedResultSet):Order = apply(o.resultName)(rs)

  def apply(o: ResultName[Order])(rs: WrappedResultSet): Order = new Order(
    id = rs.get(o.id),
    user_id = rs.get(o.user_id),
    contents = rs.get(o.contents),
    endflag = rs.get(o.endflag),
    notificationflag = rs.get(o.notificationflag),
    created = rs.get(o.created)
  )

  val o = Order.syntax("o")

  //リレーションでつながっているテーブル。複数ある場合は(u , o) = (User.u,Order.o)のように書くこと
  private val u = User.u

  def find(id: Int)(implicit session: DBSession = autoSession):Option[Order] = withSQL {
    select
      .from(Order as o)
      .leftJoin(User as u).on(o.user_id, u.id)
      .where.eq(o.id, id)
  }.map(Order(o)).single.apply()


  def findTodayOrders()(implicit session: DBSession = autoSession):List[Order] = withSQL {
    select
      .from(Order as o)
      .leftJoin(User as u).on(o.user_id, u.id)
      .where.eq(o.endflag, false).and.ge(o.created, new DateTime().toString("yyyy/MM/dd") + " 00:00:00")
  }.map(Order(o)).list.apply()

  def create(user_id: Int, contents: String, created: DateTime = DateTime.now)(implicit session: DBSession = autoSession): Order = {
    if(contents == "") throw new Exception("contentsが空です。")
    val id = withSQL {
      insert.into(Order).namedValues(
        column.user_id -> user_id,
        column.contents -> contents,
        column.created -> created
      )
    }.updateAndReturnGeneratedKey().apply()

    Order(
      id = id.toInt,
      user_id = user_id,
      contents = contents,
      user = User.find(user_id),
      endflag = false,
      notificationflag = false,
      created = created
    )
  }

  def updateEndflagTrue(order_id: Int)(implicit session: DBSession = autoSession) =
    withSQL{
      update(Order).set(
        column.endflag -> o.endflag
      ).where.eq(column.id, order_id)
    }.update.apply()

}
