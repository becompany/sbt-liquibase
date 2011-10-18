
package atd.sbtliquibase

import sbt._
import classpath._
import Process._
import Keys._

import java.io.{File, PrintStream}

import liquibase.integration.commandline.CommandLineUtils
import liquibase.resource.FileSystemResourceAccessor
import liquibase.database.Database
import liquibase.Liquibase

object LiquibasePlugin extends Plugin {
  val liquibaseConfig = config("liquibase")

  val liquibaseUpdate = TaskKey[Unit]("liquibase-update", "Run a liquibase migration")
  val liquibaseStatus = TaskKey[Unit]("liquibase-status", "Print count of unrun change sets")
  val liquibaseClearChecksums = TaskKey[Unit]("liquibase-clear-checksums", "Removes all saved checksums from database log. Useful for 'MD5Sum Check Failed' errors")
  val liquibaseListLocks      = TaskKey[Unit]("liquibase-list-locks", "Lists who currently has locks on the database changelog")
  val liquibaseReleaseLocks   = TaskKey[Unit]("liquibase-release-locks", "Releases all locks on the database changelog")
  val liquibaseValidateChangelog = TaskKey[Unit]("liquibase-validate-changelog", "Checks changelog for errors")
  val liquibaseTag = InputKey[Unit]("liquibase-tag", "Tags the current database state for future rollback")
  val liquibaseDbDiff = TaskKey[Unit]("liquibase-db-diff", "Generate changeSet(s) to make Test DB match Development")
  val liquibaseDbDoc = TaskKey[Unit]("liquibase-db-doc", "Generates Javadoc-like documentation based on current database and change log")
  val liquibaseGenerateChangelog = TaskKey[Unit]("liquibase-generate-changelog", "Writes Change Log XML to copy the current state of the database to standard out")
  val liquibaseChangelogSyncSql = TaskKey[Unit]("liquibase-changelog-sync-sql", "Writes SQL to mark all changes as executed in the database to STDOUT")

  val liquibaseRollback          = InputKey[Unit]("liquibase-rollback", "<tag> Rolls back the database to the the state is was when the tag was applied")
  val liquibaseRollbackSql       = InputKey[Unit]("liquibase-rollback-sql", "<tag> Writes SQL to roll back the database to that state it was in when the tag was applied to STDOUT")
  val liquibaseRollbackCount     = InputKey[Unit]("liquibase-rollback-count", "<num>Rolls back the last <num> change sets applied to the database")
  val liquibaseRollbackCountSql  = InputKey[Unit]("liquibase-rollback-count-sql", "<num> Writes SQL to roll back the last <num> change sets to STDOUT applied to the database")
  val liquibaseRollbackToDate    = InputKey[Unit]("liquibase-rollback-to-date", "<date> Rolls back the database to the the state is was at the given date/time. Date Format: yyyy-MM-dd HH:mm:ss")
  val liquibaseRollbackToDateSql = InputKey[Unit]("liquibase-rollback-to-date-sql", "<date> Writes SQL to roll back the database to that state it was in at the given date/time version to STDOUT")
  val liquibaseFutureRollbackSql = InputKey[Unit]("liquibase-future-rollback-sql", " Writes SQL to roll back the database to the current state after the changes in the changelog have been applied")

  val changelog         = SettingKey[String]("changelog-path", "The path to where your changelog script lives")
  val liquibaseUrl      = SettingKey[String]("liquibase-url", "The url for liquibase")
  val liquibaseUsername = SettingKey[String]("liquibase-username", "username yo.")
  val liquibasePassword = SettingKey[String]("liquibase-password", "password")
  val liquibaseDriver   = SettingKey[String]("liquibase-driver", "driver")
  val liquibaseDefaultSchemaName = SettingKey[String]("liquibase-default-schema-name","default schema name")

  lazy val liquibaseDatabase = TaskKey[Database]("liquibase-database", "the database")
  lazy val liquibase = TaskKey[Liquibase]("liquibase", "liquibase object")

  lazy val liquibaseSettings :Seq[Setting[_]] = Seq[Setting[_]](
    liquibaseDefaultSchemaName := "liquischema",
    changelog <<= baseDirectory( _ / "src" / "main" / "migrations" /  "changelog.xml" absolutePath ),

  liquibaseDatabase <<= (liquibaseUrl, liquibaseUsername, liquibasePassword, liquibaseDriver, liquibaseDefaultSchemaName, fullClasspath in Runtime ) map {
    (url :String, uname :String, pass :String, driver :String, schemaName :String, cpath ) =>
      CommandLineUtils.createDatabaseObject( ClasspathUtilities.toLoader(cpath.map(_.data)) ,url, uname, pass, driver, schemaName, null,null)
  },

  liquibase <<= ( changelog, liquibaseDatabase ) map {
    ( cLog :String, dBase :Database ) =>
      new Liquibase( cLog, new FileSystemResourceAccessor, dBase )
  },

    liquibaseUpdate <<= liquibase map { _.update(null) },
    liquibaseStatus <<= liquibase map { _.reportStatus(true, null, new LoggerWriter( ConsoleLogger() ) ) },
    liquibaseClearChecksums <<= liquibase map { _.clearCheckSums() },
    liquibaseListLocks <<= (streams, liquibase) map { (out, lbase) => lbase.reportLocks( new PrintStream(out.binary()) )  },
    liquibaseReleaseLocks <<= (streams, liquibase) map { (out, lbase) => lbase.forceReleaseLocks() },
    liquibaseValidateChangelog <<= (streams, liquibase) map { (out, lbase) => lbase.validate() },
    liquibaseDbDoc <<= ( streams, liquibase, target ) map { ( out, lbase, tdir ) =>
      lbase.generateDocumentation( tdir / "liquibase-doc" )
      out.log("Documentation generated in %s".format( tdir / "liquibase-doc" absolutePath ) },
    //liquibaseGenerateChangelog <<= (streams, liquibase, changelog ) map { (out, lbase, clog) =>  }
    // liquibaseTag
    // liquibaseDbDiff <<=
  )

  /*private def updateTask = {
    try {
      liquibase.update(null)
    } catch {
      case _ =>
    } finally {
      liquibase.getDatabase.getConnection.close
    }
  }*/

}
