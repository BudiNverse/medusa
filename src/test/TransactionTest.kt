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
    // sql statements
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
            databaseUser = "postgres"
            databasePassword = "12345".toCharArray()
            databaseUrl = "jdbc:postgresql://localhost:5432/postgres"
            driver = "org.postgresql.Driver"
            generatedKeySupport = true
        }
    }

    @Test
    fun insertTest() {
        var ins: ExecResult? = null
        transaction {
            ins = insert {
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
            ins = insert {
                statement = DummyData.insert
                values = arrayOf(DummyData.persons[2].name, DummyData.persons[2].age)
            }

            upt = update {
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
        transaction {
            qr = queryList {
                statement = DummyData.queryList
                type = ::Person
            }

            queryList<Person> {
                statement = DummyData.queryList
                type = ::Person
            }
        }

        println(qr)
    }

    @Test
    fun queryPerson() {
        var person: Person? = null
        transaction {
            person = query {
                statement = DummyData.query
                values = arrayOf("zeon000")
                type = ::Person
            }
        }

        println(person)
    }


}