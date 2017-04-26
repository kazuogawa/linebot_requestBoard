package settings

import scalikejdbc.config.DBs

trait TestDBSettings {
  DBs.setup()
}