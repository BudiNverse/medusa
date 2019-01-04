import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transaction
import com.budinverse.medusa.models.ExecResult
import com.budinverse.medusa.utils.get
import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.ResultSet


data class Person(val id: Int = 0,
                  val name: String,
                  val age: Int) {
    constructor(resultSet: ResultSet) : this(
            resultSet["id"],
            resultSet["name"],
            resultSet["age"]
    )
}

object DummyData {
    // language=MySQL
    const val insert = "INSERT INTO person(name, age) VALUES (?,?)"
    // language=MySQL
    const val query = "SELECT * FROM person WHERE name = ?"
    // language=MySQL
    const val queryList = "SELECT * FROM person"
    // language=MySQL
    const val update = "UPDATE person SET name = ?, age = ? WHERE id = ?"

    val persons = arrayOf(
            Person(name = "zeon000", age = 19),
            Person(name = "zeon111", age = 20),
            Person(name = "zeon222", age = 19),
            Person(name = "zeon333", age = 19)
    )
}


class TransactionTest {
    init {
        dbConfig {
            databaseUser = "root"
            databasePassword = "12345"
            databaseUrl = "jdbc:mysql://localhost/medusa?useLegacyDatetimeCode=false&serverTimezone=UTC"
            driver = "com.mysql.cj.jdbc.Driver"
            connectionPool = connectionPool {
                minimumIdle = 10
                maximumPoolSize = 15
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            }
        }
    }

    @Test
    fun insertTest() {
        lateinit var ins: ExecResult<Int>
        transaction {
            ins = insert {
                statement = DummyData.insert
                values = arrayOf(DummyData.persons[0].name, DummyData.persons[0].age)
                type = {
                    it[1]
                }
            }
        }

        println(ins.transformed)
        assertEquals(1, ins.transformed)
    }

    @Test
    fun updateTest() {
        lateinit var execResult: ExecResult<Person>
        transaction {
            execResult = update {
                statement = DummyData.update
                values = arrayOf(DummyData.persons[1].name, DummyData.persons[1].age, 1)
                type = ::Person
            }
        }

        assertEquals(1, execResult.rowsMutated)
    }

    @Test
    fun txn() {
        transaction {
            val insert = insert<Int> {
                statement = DummyData.insert
                values = arrayOf("jeff", 19)
                type = {
                    it[1]
                }
            }

            update<Int> {
                statement = DummyData.update
                values = arrayOf("zeon420", 19, insert.transformed)
            }

            queryList<Person> {
                statement = DummyData.queryList
                type = ::Person
            }

            query<Person> {
                statement = DummyData.query
                values = arrayOf("zeon000")
                type = ::Person
            }

            // this should break
            update<Int> {
                statement = DummyData.update
                values = arrayOf("zeon420", 20, insert.transformed)
            }
        }
    }

    @Test
    fun queryPerson() {
        lateinit var person: ExecResult<Person>
        transaction {
            person = query {
                statement = DummyData.query
                values = arrayOf("zeon111")
                type = ::Person
            }
        }

        println(person.transformed)
    }

    @Test
    fun queryListPerson() {
        lateinit var personList: ExecResult<ArrayList<Person>>
        transaction {
            personList = queryList {
                statement = DummyData.queryList
                type = ::Person
            }
        }

        println(personList.transformed)
    }
}