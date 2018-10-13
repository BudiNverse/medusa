import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transaction
import com.budinverse.medusa.core.transactionAsync
import com.budinverse.medusa.models.ExecResult
import com.budinverse.medusa.models.TransactionResult.Err
import com.budinverse.medusa.models.TransactionResult.Ok
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking {
    dbConfig {
        databaseUser = "root"
        databasePassword = "12345"
        databaseUrl = "jdbc:mysql://localhost/medusa_test?useLegacyDatetimeCode=false&serverTimezone=UTC"
        driver = "com.mysql.cj.jdbc.Driver"
    }
    launch {
        queryPersonAsync()
    }

    queryPersonAsync()
    //queryList()
    //insertAsync()
}

fun queryList() {
    lateinit var personList: List<Person>
    val txn = transaction {
        queryList<Person> {
            statement = "SELECT * FROM medusa_test.Person"
            type = ::Person
        }
    }

    when (txn) {
        is Ok -> personList = txn.res[0] as List<Person>
        is Err -> throw IllegalStateException()
    }


    println("${System.currentTimeMillis()} : $personList")
}


suspend fun queryPersonAsync() {
    var person: Person? = null
    var person2: Person? = null

    val txn1 = transactionAsync {
        query<Person> {
            statement = DummyData.query
            values = arrayOf("zeon111")
            type = ::Person
        }
    }

    val txn2 = transactionAsync {
        query<Person> {
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
    val awaitedTxn1 = txn1.await()
    val awaitedTxn2 = txn2.await()

    person = when (awaitedTxn1) {
        is Ok -> awaitedTxn1.res[0] as Person
        is Err -> null
    }

    person2 = when (awaitedTxn2) {
        is Ok -> awaitedTxn2.res[0] as Person
        is Err -> null
    }

    println("${System.currentTimeMillis()} : $person")
    println("${System.currentTimeMillis()} : $person2")

    delay(2000)
}

suspend fun insertAsync() {
    var exr: Int = -1
    lateinit var exr2: ExecResult

    val ins1 = transactionAsync {
        insert {
            statement = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
            values = arrayOf("jeff111", 19)
        }
    }

    val ins2 = transactionAsync {
        insert {
            statement = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
            values = arrayOf("jeff111", 19)
        }
    }

    println(ins1.await())
    println(ins2.await())

    println(exr)
    println(exr2)

}
