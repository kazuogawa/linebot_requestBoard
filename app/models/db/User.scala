package models.db

import scalikejdbc._
case class User(
  id: Int,
  name: String,
  lineuser_id: String
)

//後でUserに関する処理をまとめる
object User extends SQLSyntaxSupport[User]{
  override val tableName = "users"
  override val columns = Seq("id", "name", "lineuser_id")

  def apply(u: SyntaxProvider[User])(rs: WrappedResultSet):User = apply(u.resultName)(rs)
  def apply(u: ResultName[User])(rs: WrappedResultSet): User = new User(
    id = rs.get(u.id),
    name = rs.get(u.name),
    lineuser_id = rs.get(u.lineuser_id)
  )

  val u = User.syntax("u")

  //user_idでの検索
  def find(id: Int)(implicit session:DBSession = autoSession): Option[User] = withSQL{
    select.from(User as u).where.eq(u.id, id)
  }.map(User(u)).single.apply()

  //lineuser_idでの検索
  def findByLineuser_id(lineuser_id: String)(implicit session:DBSession = autoSession): Option[User] = withSQL{
    select.from(User as u).where.eq(u.lineuser_id, lineuser_id)
  }.map(User(u)).single.apply()

  def create(name: String, lineuser_id: String)(implicit session: DBSession): User = {
    if(name == "" || lineuser_id == "") throw new Exception("名前とlineuser_idは必須です。")
    //既に登録済みだったら、エラー
    val id = withSQL{
      insert.into(User).namedValues(
        column.name -> name,
        column.lineuser_id -> lineuser_id
      )
    }.updateAndReturnGeneratedKey.apply().toInt

    User(
      id = id,
      name = name,
      lineuser_id = lineuser_id
    )
  }
}