import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transaction
import com.budinverse.medusa.models.ExecResult
import com.budinverse.medusa.models.TransactionResult.Err
import com.budinverse.medusa.models.TransactionResult.Ok
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
    const val insert = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
    // language=MySQL
    const val query = "SELECT * FROM medusa_test.Person WHERE name = ?"
    // language=MySQL
    const val queryList = "SELECT * FROM medusa_test.Person"
    // language=MySQL
    const val update = "UPDATE medusa_test.Person SET name = ?, age = ? WHERE id = ?"

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
            databaseUrl = "jdbc:mysql://localhost/medusa_test?useLegacyDatetimeCode=false&serverTimezone=UTC"
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
        var ins: ExecResult<Int>? = null
        val tx = transaction {
            insert<Int> {
                statement = DummyData.insert
                values = arrayOf(DummyData.persons[0].name, DummyData.persons[0].age)
                type = {
                    it[1]
                }
            }
        }

        when (tx) {
            is Ok -> ins = tx.res[0] as ExecResult<Int>
            is Err -> println(tx.e)
        }

        println(ins?.transformed)
        assertEquals(1, ins?.rowsMutated)
    }

    @Test
    fun updateTest() {
        var rowsMutated = 0
        val tx = transaction {
            update<Int> {
                statement = DummyData.update
                values = arrayOf(DummyData.persons[1].name, DummyData.persons[1].age, 1)
            }
        }

        when (tx) {
            is Ok -> rowsMutated = (tx.res[0] as ExecResult<Int>).rowsMutated
            is Err -> println(tx.e)
        }

        assertEquals(1, rowsMutated)
    }

    @Test
    fun txn() {
        lateinit var qr: List<Person>
        var person: Person? = null
        lateinit var execResult: ExecResult<Int>
        lateinit var resList: List<Any?>
        lateinit var updateRes: ExecResult<Int>

        val transaction = transaction {
            insert<Int> {
                statement = DummyData.insert
                values = arrayOf("jeff", 19)
                type = {
                    it[1]
                }
            }

            update<Int> {
                statement = DummyData.update
                values = arrayOf("zeon420", 19, 1)
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
        }

        when (transaction) {
            is Ok -> {
                execResult = transaction.res[0] as ExecResult<Int>
                updateRes = transaction.res[1] as ExecResult<Int>
                qr = transaction.res[2] as List<Person>
                person = transaction.res[3] as? Person
                resList = transaction.res

            }
            is Err -> println(transaction.e)
        }

        //PK
        assertEquals(1, execResult.transformed)

        println(resList)
        println(execResult.transformed)
        println(qr)
        println(person)
        println(updateRes)
    }

    @Test
    fun queryPerson() {
        lateinit var person: Person
        val transaction = transaction {
            query<Person> {
                statement = DummyData.query
                values = arrayOf("zeon111")
                type = ::Person
            }
        }

        when (transaction) {
            is Ok -> person = transaction.res[0] as Person
            is Err -> throw IllegalStateException(":(")
        }

        println(person) //Person(id=2, name=zeon111, age=20)
    }

    private fun insertUser(person: Person) = transaction {
        exec<Int> {
            //language=MySQL
            statement = "UPDATE medusa_test.person SET name = ? WHERE id = ?"
            values = arrayOf(person.name, 1)
        }
    }
}