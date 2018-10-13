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


    const val insert = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
    const val query = "SELECT * FROM medusa_test.Person WHERE name = ?"
    const val queryList = "SELECT * FROM medusa_test.Person"
    const val update = "UPDATE medusa_test.Person SET name = ?, age = ? WHERE name = ?"

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
        }
    }

    @Test
    fun insertTest() {
        var ins: ExecResult? = null
        transaction {
            insert {
                statement = DummyData.insert
                values = arrayOf(DummyData.persons[0].name, DummyData.persons[0].age)
            }
        }

        println(ins)
        assertEquals(1, ins?.rowsMutated)
    }

    @Test
    fun updateTest() {
        transaction {
            update {
                statement = DummyData.update
                values = arrayOf(DummyData.persons[1].name, DummyData.persons[1].age, DummyData.persons[0].name)
            }
        }
    }

    @Test
    fun transactionTest() {
        var ins: ExecResult? = null
        var upt: ExecResult? = null

        transaction {
            insert {
                statement = DummyData.insert
                values = arrayOf(DummyData.persons[2].name, DummyData.persons[2].age)
            }

            update {
                statement = DummyData.update
                values = arrayOf(DummyData.persons[3].name, DummyData.persons[3].age, DummyData.persons[2].name)
            }
        }

        assertEquals(1, ins?.rowsMutated)
        assertEquals(1, upt?.rowsMutated)
    }

    @Test
    fun queryListTest() {
        lateinit var qr: List<Person>
        lateinit var person: Person
        lateinit var execResult: ExecResult

        val transaction = transaction {
            insert {
                statement = DummyData.insert
                values = arrayOf("jeff", 19)
            }

            queryList<Person> {
                statement = DummyData.queryList
                type = ::Person
            }

            query<Person> {
                statement = DummyData.query
                values = arrayOf("zeon111")
                type = ::Person
            }
        }

        when (transaction) {
            is Ok -> {
                execResult = transaction.res[0] as ExecResult
                qr = transaction.res[1] as List<Person>
                person = transaction.res[2] as Person

            }
            is Err -> throw IllegalStateException()
        }

        println(execResult)
        println(qr)
        println(person)
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
        exec {
            //language=MySQL
            statement = "UPDATE medusa_test.person SET name = ? WHERE id = ?"
            values = arrayOf(person.name, 1)
        }
    }
}