import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transaction
import com.budinverse.medusa.core.transactionAsync
import com.budinverse.medusa.models.ExecResult
import com.budinverse.medusa.utils.get
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking {
    dbConfig {
        databaseUser = "root"
        databasePassword = "12345"
        databaseUrl = "jdbc:mysql://localhost/medusa_test?useLegacyDatetimeCode=false&serverTimezone=UTC"
        driver = "com.mysql.cj.jdbc.Driver"
    }
    queryPersonAsync()
    //insertAsync()
}

//fun queryList(): List<Person> {
//    //lateinit var personList: List<Person>
//    transaction {
//        var personList = queryList<Person> {
//            statement = "SELECT * FROM medusa_test.Person"
//            type = ::Person
//        }
//    }
//    //return personList
//}


suspend fun queryPersonAsync() {
    var person: Person? = null
    var person2: Person? = null

    val txn1 = transactionAsync {
        person = query<Person> {
            statement = DummyData.query
            values = arrayOf("zeon111")
            type = ::Person
        }
    }

    val txn2 = transactionAsync {
        person2 = query<Person> {
            statement = DummyData.query
            values = arrayOf("zeon111")
            type = ::Person
        }
    }

    // you have to await both transactions
    // before using person and person2
    // else there will be a data race
    // and it will print null
    // which is what was initialized with
    println("${txn1.await()} || ${txn2.await()}")
    println(person)
    println(person2)
}

suspend fun insertAsync() {
    var exr: Int = -1
    lateinit var exr2: ExecResult

    val ins1 = transactionAsync {
        exr = insert {
            statement = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
            values = arrayOf("jeff111", 19)
        }.resultSet!![1] ?: -1
    }

    val ins2 = transactionAsync {
        exr2 = insert {
            statement = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
            values = arrayOf("jeff111", 19)
        }
    }

    println(ins1.await())
    println(ins2.await())

    println(exr)
    println(exr2)

}
